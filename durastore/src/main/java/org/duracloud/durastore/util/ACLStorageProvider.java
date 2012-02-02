/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.durastore.util;

import org.duracloud.common.model.AclType;
import org.duracloud.security.context.SecurityContextUtil;
import org.duracloud.security.error.NoUserLoggedInException;
import org.duracloud.security.impl.DuracloudUserDetails;
import org.duracloud.storage.error.StorageException;
import org.duracloud.storage.provider.StorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.GrantedAuthority;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * This class provides a filtering of spaces based on the username and groups
 * of the currently logged-in user. Additionally, caching of space ACLs and
 * access-type (opened/closed) is performed in this class.
 *
 * @author Andrew Woods
 *         Date: 11/22/11
 */
public class ACLStorageProvider implements StorageProvider {

    private final Logger log =
        LoggerFactory.getLogger(ACLStorageProvider.class);

    private final StorageProvider targetProvider;
    private SecurityContextUtil securityContextUtil;

    private Map<String, Map<String, AclType>> spaceACLMap;
    private Map<String, Map<String,String>> spacePropertiesMap;
    private boolean loaded;

    public ACLStorageProvider(StorageProvider targetProvider) {
        this(targetProvider, new SecurityContextUtil());
    }

    public ACLStorageProvider(StorageProvider targetProvider,
                              SecurityContextUtil securityContextUtil) {
        this.targetProvider = targetProvider;
        this.securityContextUtil = securityContextUtil;
        this.spaceACLMap = new HashMap<String, Map<String, AclType>>();
        this.spacePropertiesMap = new HashMap<String, Map<String,String>>();
        this.loaded = false;

        new Thread(new CacheLoader()).start();
    }

    /**
     * This nested class loads the cache of space ACLs and AccessTypes.
     */
    private class CacheLoader implements Runnable {
        public void run() {
            Iterator<String> spaces = targetProvider.getSpaces();
            while (spaces.hasNext()) {
                String space = spaces.next();
                spaceACLMap.put(space, getSpaceACLs(space));
                spacePropertiesMap.put(space, getSpaceProperties(space));
            }
            loaded = true;
        }

        private Map<String, AclType> getSpaceACLs(String space) {
            try {
                return targetProvider.getSpaceACLs(space);

            } catch (StorageException e) {
                log.warn("Error getting space acls: {}, err: {}", space, e);
                return new HashMap<String, AclType>();
            }
        }

        private Map<String, String> getSpaceProperties(String space) {
            try {
                return targetProvider.getSpaceProperties(space);
            } catch (StorageException e) {
                log.warn("Error getting space properties: {}, err: {}", space, e);
                return new HashMap<String, String>();
            }
        }

    }

    private void waitForCache() {
        while (!loaded) {
            log.debug("waiting: {}", targetProvider.getClass().getName());
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                // do nothing
            }
        }
    }

    /**
     * This method passes through the call to getSpaces() for 'admin' users,
     * and for 'users', only returns the list of 'open' spaces and those to
     * which they have and ACL.
     *
     * @return spaces to which the user has read permissions
     */
    @Override
    public Iterator<String> getSpaces() {
        DuracloudUserDetails user = getCurrentUserDetails();
        if (isAdmin(user)) {
            return targetProvider.getSpaces();
        }

        waitForCache();

        List<String> spaces = new ArrayList<String>();
        for (String space : spaceACLMap.keySet()) {
            Map<String, AclType> acls = spaceACLMap.get(space);
            if (userHasAccess(user, acls) && !spaces.contains(space)) {
                spaces.add(space);
            }
        }

        Collections.sort(spaces);
        return spaces.iterator();
    }

    private boolean isAdmin(DuracloudUserDetails user) {
        if (null == user) {
            return false;
        }

        GrantedAuthority[] auths = user.getAuthorities();
        if (null != auths) {
            for (GrantedAuthority auth : auths) {
                if ("ROLE_ADMIN".equals(auth.getAuthority())) {
                    return true;
                }
            }
        }
        return false;
    }

    private DuracloudUserDetails getCurrentUserDetails() {
        try {
            return securityContextUtil.getCurrentUserDetails();
        } catch (NoUserLoggedInException e) {
            return null;
        }
    }

    private boolean userHasAccess(DuracloudUserDetails user,
                                  Map<String, AclType> acls) {
        if (acls.keySet().contains(PROPERTIES_SPACE_ACL_PUBLIC)) {
            return true;
        }

        if (null == user) {
            return false;
        }

        if (acls.keySet().contains(PROPERTIES_SPACE_ACL + user.getUsername())) {
            return true;
        }

        List<String> groups = user.getGroups();
        if (null == groups || groups.size() == 0) {
            return false;
        }

        for (String group : groups) {
            if (acls.keySet().contains(PROPERTIES_SPACE_ACL + group)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Iterator<String> getSpaceContents(String spaceId, String prefix) {
        return targetProvider.getSpaceContents(spaceId, prefix);
    }

    @Override
    public List<String> getSpaceContentsChunked(String spaceId,
                                                String prefix,
                                                long maxResults,
                                                String marker) {
        return targetProvider.getSpaceContentsChunked(spaceId,
                                                      prefix,
                                                      maxResults,
                                                      marker);
    }

    @Override
    public void createSpace(String spaceId) {
        waitForCache();

        targetProvider.createSpace(spaceId);

        // Update the cache to contain current user privileges for new space, if
        // exception not thrown above.
        DuracloudUserDetails userDetails = getCurrentUserDetails();
        if (null != userDetails) {
            Map<String, AclType> acl = new HashMap<String, AclType>();
            acl.put(PROPERTIES_SPACE_ACL + userDetails.getUsername(),
                    AclType.WRITE);
            spaceACLMap.put(spaceId, acl);
        }
    }

    @Override
    public void deleteSpace(String spaceId) {
        waitForCache();

        StorageException storageException = null;
        try {
            targetProvider.deleteSpace(spaceId);
            spaceACLMap.remove(spaceId);
            spacePropertiesMap.remove(spaceId);

        } catch (StorageException e) {
            storageException = e;
        }

        // clear and reload cache if deleting: "aclstorageprovider-cache"
        if ((getClass().getSimpleName() + "-cache").equalsIgnoreCase(spaceId)) {
            log.info("cycling cache.");

            this.spaceACLMap.clear();
            this.loaded = false;

            new Thread(new CacheLoader()).start();
        }

        if (null != storageException) {
            throw storageException;
        }
    }

    @Override
    public Map<String, String> getSpaceProperties(String spaceId) {
        DuracloudUserDetails user = getCurrentUserDetails();
        if (isAdmin(user) && !loaded) {
            return targetProvider.getSpaceProperties(spaceId);
        }

        waitForCache();

        if (spacePropertiesMap.containsKey(spaceId)) {
            return spacePropertiesMap.get(spaceId);

        } else {
            Map<String, String> properties = targetProvider.getSpaceProperties(spaceId);
            spacePropertiesMap.put(spaceId, properties);
            return properties;
        }
    }

    @Override
    public void setSpaceProperties(String spaceId,
                                   Map<String, String> spaceProperties) {
        waitForCache();
        targetProvider.setSpaceProperties(spaceId, spaceProperties);
        //flush the cache 
        //In the case of properties, it is possible that what goes in is not what
        //comes out. In the case that incoming properties contain new  acls, then 
        //those new acls will not be added (according to the current implementation of 
        //storageProviderBase). 
        this.spacePropertiesMap.remove(spaceId);
    }

    @Override
    public Map<String, AclType> getSpaceACLs(String spaceId) {
        DuracloudUserDetails user = getCurrentUserDetails();
        if (isAdmin(user) && !loaded) {
            return targetProvider.getSpaceACLs(spaceId);
        }

        waitForCache();

        if (spaceACLMap.containsKey(spaceId)) {
            return spaceACLMap.get(spaceId);

        } else {
            Map<String, AclType> acls = targetProvider.getSpaceACLs(spaceId);
            spaceACLMap.put(spaceId, acls);
            return acls;
        }
    }

    @Override
    public void setSpaceACLs(String spaceId, Map<String, AclType> spaceACLs) {
        waitForCache();

        targetProvider.setSpaceACLs(spaceId, spaceACLs);

        if (null != spaceACLs) {
            // update cache
            this.spaceACLMap.put(spaceId, spaceACLs);
        }
    }

    @Override
    public String addContent(String spaceId,
                             String contentId,
                             String contentMimeType,
                             Map<String, String> userProperties,
                             long contentSize,
                             String contentChecksum,
                             InputStream content) {
        return targetProvider.addContent(spaceId,
                                         contentId,
                                         contentMimeType,
                                         userProperties,
                                         contentSize,
                                         contentChecksum,
                                         content);
    }

    @Override
    public String copyContent(String sourceSpaceId,
                              String sourceContentId,
                              String destSpaceId,
                              String destContentId) {
        return targetProvider.copyContent(sourceSpaceId,
                                          sourceContentId,
                                          destSpaceId,
                                          destContentId);
    }

    @Override
    public InputStream getContent(String spaceId, String contentId) {
        return targetProvider.getContent(spaceId, contentId);
    }

    @Override
    public void deleteContent(String spaceId, String contentId) {
        targetProvider.deleteContent(spaceId, contentId);
    }

    @Override
    public void setContentProperties(String spaceId,
                                     String contentId,
                                     Map<String, String> contentProperties) {
        targetProvider.setContentProperties(spaceId,
                                            contentId,
                                            contentProperties);
    }

    @Override
    public Map<String, String> getContentProperties(String spaceId,
                                                    String contentId) {
        return targetProvider.getContentProperties(spaceId, contentId);
    }
}

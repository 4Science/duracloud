/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.storage.provider;

import org.duracloud.common.model.AclType;
import org.duracloud.storage.error.NotFoundException;
import org.duracloud.storage.error.StorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static org.duracloud.storage.error.StorageException.NO_RETRY;

/**
 * @author: Bill Branan
 * Date: Apr 20, 2010
 */
public abstract class StorageProviderBase implements StorageProvider {

    protected static final String ACL_USER_READ = "acl-user-read";
    protected static final String ACL_USER_WRITE = "acl-user-write";
    protected static final String ACL_GROUP_READ = "acl-group-read";
    protected static final String ACL_GROUP_WRITE = "acl-group-write";
    protected static final String ACL_DELIM = ":";

    protected final Logger log = LoggerFactory.getLogger(StorageProviderBase.class);

    protected abstract boolean spaceExists(String spaceId);
    protected abstract void removeSpace(String spaceId);
    protected abstract Map<String, String> getAllSpaceProperties(String spaceId);
    protected abstract void doSetSpaceProperties(String spaceId,
                                                 Map<String, String> spaceProps);

    /**
     * This method returns all of the space properties EXCEPT the ACLs
     *
     * @param spaceId - ID of the space
     * @return map of space properties
     */
    public Map<String, String> getSpaceProperties(String spaceId) {
        Map<String, String> spaceProps = new HashMap<String, String>();
        Map<String, String> allProps = getAllSpaceProperties(spaceId);

        // ONLY include non-ACL properties.
        for (String name : allProps.keySet()) {
            if (!name.startsWith(PROPERTIES_SPACE_ACL)) {
                spaceProps.put(name, allProps.get(name));
            }
        }

        return spaceProps;
    }

    protected void setNewSpaceProperties(String spaceId,
                                         Map<String, String> spaceProperties) {
        boolean success = false;
        int maxLoops = 6;
        for (int loops = 0; !success && loops < maxLoops; loops++) {
            try {
                doSetSpaceProperties(spaceId, spaceProperties);
                success = true;
            } catch (NotFoundException e) {
                success = false;
            }
        }

        if (!success) {
            throw new StorageException(
                "Properties for space " + spaceId + " could not be created. " +
                    "The space cannot be found.");
        }
    }

    public Map<String, AclType> getSpaceACLs(String spaceId) {
        Map<String, String> allProps = getAllSpaceProperties(spaceId);
        return unpackACLs(allProps);
    }

    /*
     * Converts from packed ACL format:
     * name= acl-read
     * value= username1
     * name= acl-write
     * value= username2:username3
     *
     * to the expected ACL output format:
     * name= acl-username1
     * value= READ
     * name= acl-username2
     * value= WRITE
     * name= acl-username3
     * value= WRITE
     *
     * Also converts from packed acl-group-read and acl-group-write formats
     */
    protected Map<String, AclType> unpackACLs(Map<String, String> spaceProps) {
        Map<String, AclType> acls = new HashMap<String, AclType>();

        String readUserList = spaceProps.get(ACL_USER_READ);
        if(null != readUserList) {
            for(String userName : readUserList.split(ACL_DELIM)) {
                acls.put(PROPERTIES_SPACE_ACL + userName, AclType.READ);
            }
        }

        String writeUserList = spaceProps.get(ACL_USER_WRITE);
        if(null != writeUserList) {
            for(String userName : writeUserList.split(ACL_DELIM)) {
                acls.put(PROPERTIES_SPACE_ACL + userName, AclType.WRITE);
            }
        }

        String readGroupList = spaceProps.get(ACL_GROUP_READ);
        if(null != readGroupList) {
            for(String groupName : readGroupList.split(ACL_DELIM)) {
                acls.put(PROPERTIES_SPACE_ACL_GROUP + groupName, AclType.READ);
            }
        }

        String writeGroupList = spaceProps.get(ACL_GROUP_WRITE);
        if(null != writeGroupList) {
            for(String groupName : writeGroupList.split(ACL_DELIM)) {
                acls.put(PROPERTIES_SPACE_ACL_GROUP + groupName, AclType.WRITE);
            }
        }

        return acls;
    }

    public void setSpaceACLs(String spaceId, Map<String, AclType> spaceACLs) {
        Map<String, String> newProps = new HashMap<>();

        // get properties excluding ACLs
        Map<String, String> spaceProps = getSpaceProperties(spaceId);
        // add existing non ACLs properties
        newProps.putAll(spaceProps);

        // convert ACL format and add to props list
        newProps.putAll(packACLs(spaceACLs));

        // save
        doSetSpaceProperties(spaceId, newProps);
    }

    /*
     * Converts ACLs from the input format:
     * name= acl-username1
     * value= READ
     * name= acl-username2
     * value= WRITE
     * name= acl-username3
     * value= WRITE
     *
     * to packed ACL format:
     * name= acl-read
     * value= username1
     * name= acl-write
     * value= username2:username3
     *
     * Also converts to packed acl-group-read and acl-group-write formats
     */
    protected Map<String, String> packACLs(Map<String, AclType> spaceACLs) {
        Set<String> readUserList = new HashSet<>();
        Set<String> writeUserList = new HashSet<>();
        Set<String> readGroupList = new HashSet<>();
        Set<String> writeGroupList = new HashSet<>();

        if (null != spaceACLs) {
            for (String key : spaceACLs.keySet()) {
                AclType acl = spaceACLs.get(key);
                if(key.startsWith(PROPERTIES_SPACE_ACL_GROUP)) {
                    String groupName =
                        key.substring(PROPERTIES_SPACE_ACL_GROUP.length());
                    if(acl.equals(AclType.READ)) {
                        readGroupList.add(groupName);
                    } else if(acl.equals(AclType.WRITE)) {
                        writeGroupList.add(groupName);
                    }
                } else if (key.startsWith(PROPERTIES_SPACE_ACL)) {
                    String userName =
                        key.substring(PROPERTIES_SPACE_ACL.length());
                    if(acl.equals(AclType.READ)) {
                        readUserList.add(userName);
                    } else if(acl.equals(AclType.WRITE)) {
                        writeUserList.add(userName);
                    }
                }
            }
        }

        Map<String, String> packedAcls = new HashMap<>();
        includeACL(packedAcls, ACL_USER_READ, readUserList);
        includeACL(packedAcls, ACL_USER_WRITE, writeUserList);
        includeACL(packedAcls, ACL_GROUP_READ, readGroupList);
        includeACL(packedAcls, ACL_GROUP_WRITE, writeGroupList);

        return packedAcls;
    }

    private void includeACL(Map<String, String> packedAcls,
                            String acl,
                            Set<String> listEntries) {
        if(!listEntries.isEmpty()) {
            StringBuilder aclValues = new StringBuilder();
            for(String aclValue : listEntries) {
                aclValues.append(aclValue).append(ACL_DELIM);
            }
            packedAcls.put(acl, aclValues.toString());
        }
    }

    protected void throwIfSpaceExists(String spaceId) {
        if (spaceExists(spaceId)) {
            String msg = "Error: Space already exists: " + spaceId;
            throw new StorageException(msg, NO_RETRY);
        }
    }

    protected void throwIfSpaceNotExist(String spaceId) {
        throwIfSpaceNotExist(spaceId, true);
    }

    protected void throwIfSpaceNotExist(String spaceId, boolean wait) {
        if (!spaceExists(spaceId)) {
            String msg = "Error: Space does not exist: " + spaceId;
            if (wait) {
                waitForSpaceAvailable(spaceId);
                if (!spaceExists(spaceId)) {
                    throw new NotFoundException(msg);
                }
            } else {
                throw new NotFoundException(msg);
            }
        }
    }

    private void waitForSpaceAvailable(String spaceId) {
        int maxLoops = 6;
        for (
            int loops = 0; !spaceExists(spaceId) && loops < maxLoops; loops++) {
            try {
                log.debug(
                    "Waiting for space " + spaceId + " to be available, loop " +
                        loops);
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                log.warn(e.getMessage(), e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void deleteSpace(String spaceId) {
        log.debug("deleteSpace(" + spaceId + ")");
        throwIfSpaceNotExist(spaceId);

        Map<String, String> allProps = getAllSpaceProperties(spaceId);
        allProps.put("is-delete", "true");
        doSetSpaceProperties(spaceId, allProps);

        SpaceDeleteWorker deleteThread = getSpaceDeleteWorker(spaceId);
        new Thread(deleteThread).start();
    }

    protected class SpaceDeleteWorker implements Runnable {
        protected final Logger log =
            LoggerFactory.getLogger(SpaceDeleteWorker.class);

        private String spaceId;

        public SpaceDeleteWorker(String spaceId) {
            this.spaceId = spaceId;
        }

        @Override
        public void run() {
            Iterator<String> contents = getSpaceContents(spaceId, null);
            int count = 0;

            while(contents.hasNext() && count++ < 5) {
                try{
                    Thread.sleep((long)Math.pow(2,count) * 100);
                } catch(InterruptedException e) {
                }

                while(contents.hasNext()) {
                    String contentId = contents.next();
                    log.debug("deleteContent(" + spaceId + ", " +
                              contentId + ") - count=" + count);

                    try {
                        deleteContent(spaceId, contentId);
                    } catch(Exception e) {
                        log.error("Error deleting content " + contentId +
                            " in space " + spaceId, e);
                    }
                }
                contents = getSpaceContents(spaceId, null);
            }

            if(contents.hasNext()) {
                log.debug("deleteSpaceContents(" + spaceId +
                          ") exceeded retries");

                Map<String, String> allProps = getAllSpaceProperties(spaceId);
                allProps.put("delete-error", "Unable to delete all contents");
                doSetSpaceProperties(spaceId, allProps);
            }
            else {
                log.debug("removeSpace(" + spaceId + ")");
                removeSpace(spaceId);
            }
        }
    }

    public SpaceDeleteWorker getSpaceDeleteWorker(String spaceId) {
        return new SpaceDeleteWorker(spaceId);
    }
}

/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.security.vote;

import org.duracloud.security.domain.HttpVerb;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.Authentication;
import org.springframework.security.ConfigAttributeDefinition;
import org.springframework.security.providers.anonymous.AnonymousAuthenticationToken;
import org.springframework.security.userdetails.UserDetailsService;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

import static org.duracloud.security.vote.VoterUtil.debugText;

/**
 * This class decides if a caller has WRITE access to a given resource. If the
 * caller is seeking READ access to this resource, this class abstains from
 * casting a vote.
 *
 * @author Andrew Woods
 *         Date: 11/18/11
 */
public class SpaceWriteAccessVoter extends SpaceAccessVoter {

    private final Logger log =
        LoggerFactory.getLogger(SpaceReadAccessVoter.class);

    public SpaceWriteAccessVoter(ContentStoreUtil contentStoreUtil,
                                 UserDetailsService userDetailsService) {
        super(contentStoreUtil, userDetailsService);
    }

    /**
     * This method checks the ACL state of the arg resource
     * (space and provider) and denies access to principals if they are
     * anonymous or if they do not have a WRITE ACL for the space.
     *
     * @param auth     principal seeking AuthZ
     * @param resource that is under protection
     * @param config   access-attributes defined on resource
     * @return vote (AccessDecisionVoter.ACCESS_GRANTED, ACCESS_DENIED, ACCESS_ABSTAIN)
     */
    public int vote(Authentication auth,
                    Object resource,
                    ConfigAttributeDefinition config) {
        String label = "SpaceWriteAccessVoterImpl";
        if (resource != null && !supports(resource.getClass())) {
            log.debug(debugText(label, auth, config, resource, ACCESS_ABSTAIN));
            return ACCESS_ABSTAIN;
        }

        HttpServletRequest httpRequest = getHttpServletRequest(resource);
        if (null == httpRequest) {
            log.debug(debugText(label, auth, config, resource, ACCESS_DENIED));
            return ACCESS_DENIED;
        }

        HttpVerb verb = getHttpVerb(httpRequest);
        if (null == verb) {
            log.debug(debugText(label, auth, config, resource, ACCESS_DENIED));
            return ACCESS_DENIED;
        }

        // This class only handles HTTP write verbs.
        if (verb.isRead()) {
            log.debug(debugText(label, auth, config, resource, ACCESS_ABSTAIN));
            return ACCESS_ABSTAIN;
        }

        // Anonymous users can not WRITE spaces.
        if (auth instanceof AnonymousAuthenticationToken) {
            log.debug(debugText(label, auth, config, resource, ACCESS_DENIED));
            return ACCESS_DENIED;
        }

        // The Admin always has WRITE access.
        if (isAdmin(auth.getName())) {
            log.debug(debugText(label, auth, config, resource, ACCESS_GRANTED));
            return ACCESS_GRANTED;
        }

        Map<String, String> acls = getSpaceACLs(httpRequest);

        // Null means space does not exist, GRANT permission to create it.
        if (null == acls && verb.equals(HttpVerb.PUT)) {
            log.debug(debugText(label, auth, config, resource, ACCESS_GRANTED));
            return ACCESS_GRANTED;
        }

        if (hasWriteAccess(auth.getName(), acls)) {
            log.debug(debugText(label, auth, config, resource, ACCESS_GRANTED));
            return ACCESS_GRANTED;
        }

        List<String> userGroups = getUserGroups(auth);
        if (groupsHaveWriteAccess(userGroups, acls)) {
            log.debug(debugText(label, auth, config, resource, ACCESS_GRANTED));
            return ACCESS_GRANTED;
        }

        int grant = ACCESS_DENIED;
        log.debug(debugText(label, auth, config, resource, grant));
        return grant;
    }

}
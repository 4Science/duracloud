/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.duraboss.rest;

import org.duracloud.common.rest.RestUtil;
import org.duracloud.security.DuracloudUserDetailsService;
import org.duracloud.security.domain.SecurityConfigBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import static org.duracloud.security.xml.SecurityDocumentBinding.createSecurityConfigFrom;

/**
 * @author Andrew Woods
 *         Date: Apr 15, 2010
 */
@Path("/security")
public class SecurityRest extends BaseRest {
    private final Logger log = LoggerFactory.getLogger(SecurityRest.class);

    private DuracloudUserDetailsService userDetailsService;
    private RestUtil restUtil;

    public SecurityRest(DuracloudUserDetailsService userDetailsService,
                        RestUtil restUtil) {
        this.userDetailsService = userDetailsService;
        this.restUtil = restUtil;
    }

    @POST
    public Response initializeUsers() {
        RestUtil.RequestContent content = null;
        try {
            content = restUtil.getRequestContent(request, headers);
            SecurityConfigBean config =
                createSecurityConfigFrom(content.getContentStream());
            userDetailsService.initialize(config.getLdapConfig(),
                                          config.getAcctIds());

            String responseText = "Initialization Successful\n";
            return Response.ok(responseText, TEXT_PLAIN).build();
            
        } catch (Exception e) {
            log.error("Error: initializing users.", e);
            String entity = e.getMessage() == null ? "null" : e.getMessage();
            return Response.serverError().entity(entity).build();
        }
    }
}

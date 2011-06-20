/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.duraservice.aop;

import org.duracloud.serviceapi.aop.ServiceMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GetDeployedServicesAdvice extends ServiceAdvice {

    private final Logger log = LoggerFactory.getLogger(GetDeployedServicesAdvice.class);

    @Override
    protected ServiceMessage createServiceMessage(Object[] methodArgs) {
        return new ServiceMessage();
    }

}

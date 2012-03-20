/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.durastore.aop;

import org.duracloud.storage.aop.ContentStoreMessage;
import org.duracloud.storage.aop.SpaceMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpaceDeleteAdvice extends BaseContentStoreAdvice {

    private final Logger log = LoggerFactory.getLogger(SpaceDeleteAdvice.class);

    protected static final int STORE_ID_INDEX = 1;
    protected static final int SPACE_ID_INDEX = 2;

    @Override
    protected Logger log() {
        return log;
    }

    @Override
    protected ContentStoreMessage createMessage(Object[] methodArgs) {
        String storeId = getStoreId(methodArgs);
        String spaceId = getSpaceId(methodArgs);

        SpaceMessage msg = new SpaceMessage();
        msg.setStoreId(storeId);
        msg.setSpaceId(spaceId);
        msg.setUsername(getCurrentUsername());

        return msg;
    }

    private String getStoreId(Object[] methodArgs) {
        log.debug("Returning 'storeId' at index: " + STORE_ID_INDEX);
        return (String) methodArgs[STORE_ID_INDEX];
    }

    private String getSpaceId(Object[] methodArgs) {
        log.debug("Returning 'spaceId' at index: " + SPACE_ID_INDEX);
        return (String) methodArgs[SPACE_ID_INDEX];
    }

}
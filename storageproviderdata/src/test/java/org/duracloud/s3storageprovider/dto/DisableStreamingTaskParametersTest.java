/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.s3storageprovider.dto;

import org.duracloud.error.TaskDataException;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.junit.matchers.JUnitMatchers.containsString;

/**
 * @author Bill Branan
 *         Date: 3/5/15
 */
public class DisableStreamingTaskParametersTest {

    private final String spaceId = "space-id";

    @Test
    public void testSerialize() {
        DisableStreamingTaskParameters taskParams =
            new DisableStreamingTaskParameters();
        taskParams.setSpaceId(spaceId);

        String result = taskParams.serialize();
        String cleanResult = result.replaceAll("\\s+", "");
        assertThat(cleanResult, containsString("\"spaceId\":\""+spaceId +"\""));
    }

    @Test
    public void testDeserialize() {
        // Verify valid params
        String taskParamsSerialized = "{\"spaceId\" : \""+spaceId+"\"}";

        DisableStreamingTaskParameters taskParams =
            DisableStreamingTaskParameters.deserialize(taskParamsSerialized);
        assertEquals(spaceId, taskParams.getSpaceId());

        // Verify that empty params throw
        taskParamsSerialized = "{\"spaceId\" : \"\"}";

        try {
            DisableStreamingTaskParameters.deserialize(taskParamsSerialized);
            fail("Exception expected: Invalid params");
        } catch(TaskDataException e) {
        }

        // Verify that empty params throw
        try {
            DisableStreamingTaskParameters.deserialize("");
            fail("Exception expected: Invalid params");
        } catch(TaskDataException e) {
        }
    }

}

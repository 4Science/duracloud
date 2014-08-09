/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.client.task;

import org.duracloud.client.ContentStore;
import org.duracloud.error.ContentStoreException;
import org.duracloud.snapshot.SnapshotConstants;
import org.duracloud.snapshot.dto.task.CompleteSnapshotTaskParameters;
import org.duracloud.snapshot.dto.task.CompleteSnapshotTaskResult;
import org.duracloud.snapshot.dto.task.CreateSnapshotTaskParameters;
import org.duracloud.snapshot.dto.task.CreateSnapshotTaskResult;
import org.duracloud.snapshot.dto.task.GetRestoreTaskParameters;
import org.duracloud.snapshot.dto.task.GetRestoreTaskResult;
import org.duracloud.snapshot.dto.task.GetSnapshotListTaskResult;
import org.duracloud.snapshot.dto.task.GetSnapshotTaskParameters;
import org.duracloud.snapshot.dto.task.GetSnapshotTaskResult;
import org.duracloud.snapshot.dto.task.RestoreSnapshotTaskParameters;
import org.duracloud.snapshot.dto.task.RestoreSnapshotTaskResult;

/**
 * Implements the snapshot task client interface by making task calls through
 * a ContentStore.
 *
 * @author Bill Branan
 *         Date: 8/8/14
 */
public class SnapshotTaskClientImpl implements SnapshotTaskClient {

    private ContentStore contentStore;

    public SnapshotTaskClientImpl(ContentStore contentStore) {
        this.contentStore = contentStore;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CreateSnapshotTaskResult createSnapshot(String spaceId,
                                                   String description,
                                                   String userEmail)
        throws ContentStoreException {
        CreateSnapshotTaskParameters taskParams =
            new CreateSnapshotTaskParameters();
        taskParams.setSpaceId(spaceId);
        taskParams.setDescription(description);
        taskParams.setUserEmail(userEmail);

        String taskResult =
            contentStore.performTask(SnapshotConstants.CREATE_SNAPSHOT_TASK_NAME,
                                     taskParams.serialize());

        return CreateSnapshotTaskResult.deserialize(taskResult);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GetSnapshotTaskResult getSnapshot(String snapshotId)
        throws ContentStoreException {
        GetSnapshotTaskParameters taskParams = new GetSnapshotTaskParameters();
        taskParams.setSnapshotId(snapshotId);

        String taskResult =
            contentStore.performTask(SnapshotConstants.GET_SNAPSHOT_TASK_NAME,
                                     taskParams.serialize());

        return GetSnapshotTaskResult.deserialize(taskResult);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompleteSnapshotTaskResult completeSnapshot(String spaceId)
        throws ContentStoreException {
        CompleteSnapshotTaskParameters taskParams =
            new CompleteSnapshotTaskParameters();
        taskParams.setSpaceId(spaceId);

        String taskResult =
            contentStore.performTask(SnapshotConstants.COMPLETE_SNAPSHOT_TASK_NAME,
                                     taskParams.serialize());

        return CompleteSnapshotTaskResult.deserialize(taskResult);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GetSnapshotListTaskResult getSnapshots()
        throws ContentStoreException {
        String taskResult =
            contentStore.performTask(SnapshotConstants.GET_SNAPSHOTS_TASK_NAME, "");

        return GetSnapshotListTaskResult.deserialize(taskResult);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void getSnapshotContents() throws ContentStoreException {
        // TODO: Implement
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RestoreSnapshotTaskResult restoreSnapshot(String snapshotId,
                                                     String userEmail)
        throws ContentStoreException {
        RestoreSnapshotTaskParameters taskParams =
            new RestoreSnapshotTaskParameters();
        taskParams.setSnapshotId(snapshotId);
        taskParams.setUserEmail(userEmail);

        String taskResult =
            contentStore.performTask(SnapshotConstants.RESTORE_SNAPSHOT_TASK_NAME,
                                     taskParams.serialize());

        return RestoreSnapshotTaskResult.deserialize(taskResult);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GetRestoreTaskResult getRestore(Long restoreId)
        throws ContentStoreException {
        GetRestoreTaskParameters taskParams = new GetRestoreTaskParameters();
        taskParams.setRestoreId(restoreId);

        String taskResult =
            contentStore.performTask(SnapshotConstants.GET_RESTORE_TASK_NAME,
                                     taskParams.serialize());
        return GetRestoreTaskResult.deserialize(taskResult);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GetRestoreTaskResult getRestoreBySnapshot(String snapshotId)
        throws ContentStoreException {
        GetRestoreTaskParameters taskParams = new GetRestoreTaskParameters();
        taskParams.setSnapshotId(snapshotId);

        String taskResult =
            contentStore.performTask(SnapshotConstants.GET_RESTORE_TASK_NAME,
                                     taskParams.serialize());
        return GetRestoreTaskResult.deserialize(taskResult);
    }

}

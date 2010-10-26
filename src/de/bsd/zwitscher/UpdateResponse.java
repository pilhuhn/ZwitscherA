package de.bsd.zwitscher;

import twitter4j.Status;
import twitter4j.StatusUpdate;

/**
 * Response object for any update sent to the
 * server.
 *
 * @author Heiko W. Rupp
 */
public class UpdateResponse {

    StatusUpdate update;
    long id;
    String message;
    boolean success;
    UpdateType updateType;
    Status status;

    public UpdateResponse(UpdateType updateType, boolean success, String message) {
        this.message = message;
        this.success = success;
        this.updateType = updateType;
    }

    public UpdateResponse(UpdateType updateType, StatusUpdate update) {
        this.update = update;
        this.updateType = updateType;
    }

    public UpdateResponse(UpdateType updateType, Status status) {
        this.updateType = updateType;
        this.status = status;
    }

    public UpdateResponse(UpdateType updateType, long id) {
        this.updateType = updateType;
        this.id = id;
    }

    public UpdateType getUpdateType() {
        return updateType;
    }

    public StatusUpdate getUpdate() {
        return update;
    }

    public String getMessage() {
        return message;
    }

    public boolean isSuccess() {
        return success;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setUpdate(StatusUpdate update) {
        this.update = update;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}

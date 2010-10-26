package de.bsd.zwitscher;

import twitter4j.Status;
import twitter4j.StatusUpdate;

/**
 * // TODO: Document this
 *
 * @author Heiko W. Rupp
 */
public class UpdateRequest {

    UpdateType updateType;
    String message;
    long id;
    StatusUpdate statusUpdate;
    Status status;

    public UpdateRequest(UpdateType updateType) {
        this.updateType = updateType;
    }
}

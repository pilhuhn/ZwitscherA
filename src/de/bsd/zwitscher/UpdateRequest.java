package de.bsd.zwitscher;

import java.io.Serializable;

import android.view.View;
import twitter4j.Status;
import twitter4j.StatusUpdate;

/**
 * A request that is given to the {@link de.bsd.zwitscher.UpdateStatusTask}
 *
 * @author Heiko W. Rupp
 */
public class UpdateRequest implements Serializable {

    private static final long serialVersionUID = -1L;

    UpdateType updateType;
    String message;
    long id;
    StatusUpdate statusUpdate;
    Status status;
    String picturePath;
    transient View view; // Don't serialize
    String url;         // for external apps
    String extUser;     // for external apps
    String extPassword; // for external apps

    public UpdateRequest(UpdateType updateType) {
        this.updateType = updateType;
    }

    public UpdateType getUpdateType() {
        return updateType;
    }
}

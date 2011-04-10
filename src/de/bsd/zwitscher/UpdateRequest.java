package de.bsd.zwitscher;

import android.view.View;
import twitter4j.Status;
import twitter4j.StatusUpdate;

/**
 * A request that is given to the {@link de.bsd.zwitscher.UpdateStatusTask}
 *
 * @author Heiko W. Rupp
 */
public class UpdateRequest {

    UpdateType updateType;
    String message;
    long id;
    StatusUpdate statusUpdate;
    Status status;
    String picturePath;
    View view;
    String url;         // for external apps
    String extUser;     // for extranal apps
    String extPassword; // for external apps

    public UpdateRequest(UpdateType updateType) {
        this.updateType = updateType;
    }
}

package de.bsd.zwitscher.h;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import de.bsd.zwitscher.DownloadUserImageTask;
import de.bsd.zwitscher.R;
import de.bsd.zwitscher.TwitterHelper;
import de.bsd.zwitscher.account.Account;
import de.bsd.zwitscher.account.AccountHolder;
import de.bsd.zwitscher.helper.NetworkHelper;
import twitter4j.Place;
import twitter4j.Status;

/**
 * // TODO: Document this
 * @author Heiko W. Rupp
 */
public class OneTweetFragment extends Fragment {

    private Status status;
    private View view;
    private ImageView userPictureView;
    private boolean downloadPictures;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        System.out.println("OTF: on create view");

        if (container==null)
            return null;

        view = inflater.inflate(R.layout.single_tweet, container,false);

        userPictureView = (ImageView) view.findViewById(R.id.UserPictureImageView);

        NetworkHelper networkHelper = new NetworkHelper(getActivity());
        downloadPictures = networkHelper.mayDownloadImages();


        return view;

    }

    void setStatus(Status status) {
        this.status = status;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);    // TODO: Customise this generated block

        Account account = AccountHolder.getInstance().getAccount();
        /*
         * Block is copied over from One Tweet Activity - TODO refactor in a helper
         */
        if (status.getRetweetedStatus()==null)
            new DownloadUserImageTask(userPictureView,downloadPictures).execute(status.getUser());
        else
            new DownloadUserImageTask(userPictureView,downloadPictures).execute(status.getRetweetedStatus().getUser());

        TextView tv01 = (TextView) view.findViewById(R.id.TextView01);
        StringBuilder sb = new StringBuilder("<b>");
        if (status.getRetweetedStatus()==null) {
            sb.append(status.getUser().getName());
            sb.append(" (");
            sb.append(status.getUser().getScreenName());
            sb.append(")");
            sb.append("</b>");
        }
        else {
            sb.append(status.getRetweetedStatus().getUser().getName());
            sb.append(" (");
            sb.append(status.getRetweetedStatus().getUser().getScreenName());
            sb.append(" )</b> ").append(getString(R.string.resent_by)).append(" <b>");
            sb.append(status.getUser().getName());
            sb.append("</b>");
        }
        tv01.setText(Html.fromHtml(sb.toString()));

        TextView mtv = (TextView) view.findViewById(R.id.MiscTextView);
        if (status.getInReplyToScreenName()!=null) {
            String s = getString(R.string.in_reply_to);
            mtv.setText(Html.fromHtml(s + " <b>" + status.getInReplyToScreenName() + "</b>"));
        }
        else {
            mtv.setText("");
        }

        TextView tweetView = (TextView)view.findViewById(R.id.TweetTextView);
        tweetView.setText(status.getText());

        TextView timeClientView = (TextView)view.findViewById(R.id.TimeTextView);
        TwitterHelper th = new TwitterHelper(getActivity(), account);
        String s = getString(R.string.via);
        String text = th.getStatusDate(status) + s + status.getSource();
        String from = getString(R.string.from);
        if (status.getPlace()!=null) {
            Place place = status.getPlace();
            text += " " + from + " " ;
            if (place.getFullName()!=null)
                text += "<a href=\"geo:0,0?q=" + place.getFullName() + ","+ place.getCountry() + "\">";
            text += place.getFullName();
            if (place.getFullName()!=null)
                text += "</a>";

        }
        timeClientView.setText(Html.fromHtml(text));
        timeClientView.setMovementMethod(LinkMovementMethod.getInstance());

    }
}

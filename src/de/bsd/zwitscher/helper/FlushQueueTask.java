package de.bsd.zwitscher.helper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.List;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;
import de.bsd.zwitscher.TweetDB;
import de.bsd.zwitscher.TwitterHelper;
import de.bsd.zwitscher.UpdateRequest;
import de.bsd.zwitscher.UpdateResponse;
import de.bsd.zwitscher.UpdateType;
import de.bsd.zwitscher.account.Account;

/**
 * A task that sends stuff that got queued while being offline.
 * @author Heiko W. Rupp
 */
public class FlushQueueTask extends AsyncTask<Void,Integer,Pair<Integer,Integer>> {

    private Context context;
    private Account account;

    public FlushQueueTask(Context context, Account account) {
        this.context = context;
        this.account = account;
    }

    @Override
    protected Pair<Integer,Integer> doInBackground(Void... voids) {

        NetworkHelper nh = new NetworkHelper(context);
        if (!nh.isOnline())
            return new Pair<Integer, Integer>(0,0);

        TweetDB tdb = new TweetDB(context,account.getId());
        TwitterHelper th = new TwitterHelper(context,account);
        List<Pair<Integer,byte[]>> list = tdb.getUpdatesForAccount();
        Integer count = list.size();
        int good=0;
        for (Pair pair : list) {
             try {
                 byte[] bytes = (byte[]) pair.second;
                 ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes));
                 UpdateRequest usr = (UpdateRequest) ois.readObject();
                 ois.close();

                 UpdateResponse ret=null;

                 switch (usr.getUpdateType()) {
                 case UPDATE:
                     ret = th.updateStatus(usr);
                     good++;
                     break;
                 case FAVORITE:
                     ret = th.favorite(usr);
                     good++;
                     break;
                 case DIRECT:
                     ret = th.direct(usr);
                     good++;
                     break;
                 case RETWEET:
                     ret = th.retweet(usr);
                     good++;
                     break;

                 default:
                     Log.e("FlushQueueTask","Update type " + usr.getUpdateType() + " not yet supported");
                 }
                 Log.i("FlushQueueTask","Return was: " + ret);
                 if (ret!=null && ret.getStatusCode() != 502) { // TODO look up codes
                     tdb.removeUpdate((Integer) pair.first);
                 }

             } catch (IOException e) {
                 e.printStackTrace();  // TODO: Customise this generated block
             } catch (ClassNotFoundException e) {
                 e.printStackTrace();  // TODO: Customise this generated block
             }

         }

        Pair<Integer,Integer> result = new Pair<Integer, Integer>(count,good);
        return result;
    }

    @Override
    protected void onPostExecute(Pair<Integer,Integer> result) {
        super.onPostExecute(result);
        if (result.first>0)
            Toast.makeText(context,"Successfully sent " + result.second + " items out of " + result.first,Toast.LENGTH_LONG).show();
    }
}

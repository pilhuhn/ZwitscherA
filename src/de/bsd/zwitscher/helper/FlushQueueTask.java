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
import de.bsd.zwitscher.*;
import de.bsd.zwitscher.account.Account;
import de.bsd.zwitscher.other.ReadItLaterStore;
import twitter4j.TwitterException;

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

        TweetDB tdb = TweetDB.getInstance(context);
        TwitterHelper th = new TwitterHelper(context,account);
        List<Pair<Integer,byte[]>> list = tdb.getUpdatesForAccount(account.getId());
        Integer count = list.size();
        int good=0;

        for (Pair pair : list) {
             try {
                 UpdateRequest usr = liquifyUpdateRequest(pair);

                 UpdateResponse ret= new UpdateResponse(usr.getUpdateType());

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
                 case REPORT_AS_SPAMMER:
                     ret.setId(usr.id);
                     th.reportAsSpammer(usr.id);
                     good++;
                     break;
                 case LATER_READING:
                     good = sendToPocket(good, usr, ret);

                     break;
                 }
                 Log.i("FlushQueueTask","Return was: " + ret);
                 if (ret!=null && ret.getStatusCode() != 502) { // TODO look up codes
                     tdb.removeUpdate((Integer) pair.first);
                 }

             } catch (IOException e) {
                 e.printStackTrace();  // TODO: Customise this generated block
             } catch (ClassNotFoundException e) {
                 e.printStackTrace();  // TODO: Customise this generated block
             } catch (TwitterException e) {
                 e.printStackTrace();  // TODO: Customise this generated block
             }

        }

        Pair<Integer,Integer> result = new Pair<Integer, Integer>(count,good);
        return result;
    }

    private int sendToPocket(int good, UpdateRequest usr, UpdateResponse ret) {
        ReadItLaterStore store = new ReadItLaterStore(usr.extUser,usr.extPassword);
        String result = store.store(usr.status,!account.isStatusNet(),usr.url);
        boolean success;
        success = result.startsWith("200");

        ret.setMessage(result);

        if (success) {
            good++;
            ret.setSuccess();
        }
        return good;
    }

    @Override
    protected void onPostExecute(Pair<Integer,Integer> result) {
        super.onPostExecute(result);
        if (result.first>0) {
            String s = context.getResources().getString(R.string.successfully_sent,result.second,result.first);
            Toast.makeText(context,s,Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Extract the UpdateRequest from the passed pair.
     * @param pair The pair to extract the request from
     * @return The Request
     */
    private UpdateRequest liquifyUpdateRequest(Pair pair) throws IOException, ClassNotFoundException {
        byte[] bytes = (byte[]) pair.second;
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes));
        UpdateRequest usr = (UpdateRequest) ois.readObject();
        ois.close();
        return usr;
    }

}

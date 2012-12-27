package de.bsd.zwitscher;


import android.*;
import android.R;
import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;

/**
 * // TODO: Document this
 *
 * @author Heiko W. Rupp
 */
public class MainTabListener<T extends Fragment> implements ActionBar.TabListener {

    private Fragment fragment;
    private Activity activity;
    private String tag;
    private Class<TweetListFragment> fragmentClass;

    public MainTabListener(Activity activity, String tag, Class<TweetListFragment> fragmentClass) {
        this.activity = activity;
        this.tag = tag;
        this.fragmentClass = fragmentClass;
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {

        if (fragment==null) {
            fragment = Fragment.instantiate(activity,fragmentClass.getName());
            if (fragment instanceof TweetListFragment) {
                ((TweetListFragment)fragment).setTag(tag);
            }
            ft.add(R.id.content,fragment,tag);
        }
        else {
            ft.attach(fragment);
        }
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);

    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
        if (fragment != null) {
         // Detach the fragment, because another one is being attached
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE);
         ft.detach(fragment);
        }
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
        // TODO: Customise this generated block
    }
}

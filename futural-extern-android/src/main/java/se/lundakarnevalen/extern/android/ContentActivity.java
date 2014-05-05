package se.lundakarnevalen.extern.android;

import android.app.Dialog;
import android.content.res.Resources;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.view.Gravity;
import android.view.View.OnClickListener;
import android.graphics.Color;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.readystatesoftware.systembartint.SystemBarTintManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import se.lundakarnevalen.extern.data.DataType;
import se.lundakarnevalen.extern.fragments.FoodFragment;
import se.lundakarnevalen.extern.fragments.FunFragment;
import se.lundakarnevalen.extern.fragments.MapFragment;
import se.lundakarnevalen.extern.fragments.OtherFragment;
import se.lundakarnevalen.extern.fragments.SchemeFragment;
import se.lundakarnevalen.extern.fragments.TrainMapFragment;
import se.lundakarnevalen.extern.map.MarkerType;
import se.lundakarnevalen.extern.widget.LKRightMenuArrayAdapter;
import se.lundakarnevalen.extern.widget.LKRightMenuArrayAdapter.*;

import static se.lundakarnevalen.extern.util.ViewUtil.*;

public class ContentActivity extends ActionBarActivity {
    public static final String TAG_MAP = "map";

    public static final String LOG_TAG = ContentActivity.class.getSimpleName();

    private FragmentManager fragmentMgr;
    private LKRightMenuArrayAdapter adapter;

    private View mActionBarView;
    private ListView rightMenuList;
    private DrawerLayout drawerLayout;
    private BottomMenuClickListener listener;

    public MapFragment mapFragment;

    public <T> T find(int id, Class<T> clz) {
        return clz.cast(findViewById(id));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_content);
        fragmentMgr = getSupportFragmentManager();

        mapFragment = new MapFragment();
        loadFragmentWithReplace(mapFragment);

        rightMenuList = find(R.id.right_menu_list, ListView.class);

        setupDrawerLayout();
        setupActionbar();
        setupTint();

        populateBottomMenu(find(R.id.bottom_frame_menu, LinearLayout.class));

        //Load populate with delay
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                populateRightMenuDrawer();
            }
        }, 300);


        // TODO modify design
        // createCustomDialog();
    }

    private void setupDrawerLayout() {
        drawerLayout = find(R.id.drawer_layout, DrawerLayout.class);
        drawerLayout.setScrimColor(Color.TRANSPARENT);
    }

    public void toggleRightDrawer(){
        if (drawerLayout.isDrawerOpen(Gravity.RIGHT)) {
            drawerLayout.closeDrawer(Gravity.RIGHT);
        } else {
            drawerLayout.openDrawer(Gravity.RIGHT);
        }
    }

    private void createCustomDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.welcome_dialog);
        dialog.setTitle("WELCOME");
        // set the custom dialog components - text, image and button
        TextView text = (TextView) dialog.findViewById(R.id.text);
        text.setText("Android custom dialog example!");

        Button dialogButton = (Button) dialog.findViewById(R.id.okButton);
        // if button is clicked, close the custom dialog
        dialogButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();

    }

    @Override
    protected void onDestroy() {
        Log.d(LOG_TAG, "onDestroy()!? null => preloaded");
        MapFragment.clean();
        TrainMapFragment.clean();
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        return super.onOptionsItemSelected(item);
    }



    private void setupTint() {
        SystemBarTintManager tintManager = new SystemBarTintManager(this);
        tintManager.setStatusBarTintEnabled(true);
        tintManager.setStatusBarTintColor(getResources().getColor(R.color.red));
        tintManager.setNavigationBarTintEnabled(true);
        tintManager.setNavigationBarTintColor(getResources().getColor(R.color.red));
    }

    private ActionBar setupActionbar() {
        LayoutInflater inflater = LayoutInflater.from(this);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        View view = mActionBarView = inflater.inflate(R.layout.action_bar_layout, null);
        actionBar.setCustomView(view);
        return actionBar;
    }

    private void populateBottomMenu(LinearLayout bottomMenu) {
        listener = new BottomMenuClickListener();
        AtomicInteger counter = new AtomicInteger(0);
        createBottomMenuItem(bottomMenu, listener, counter, new FunFragment(), R.id.button1, R.string.fun, R.drawable.fun_logo);
        createBottomMenuItem(bottomMenu, listener, counter, new FoodFragment(), R.id.button2, R.string.food, R.drawable.food_logo);
        createBottomMenuItem(bottomMenu, listener, counter, mapFragment, R.id.button3, R.string.map, R.drawable.map_logo);
        createBottomMenuItem(bottomMenu, listener, counter, new SchemeFragment(), R.id.button4, R.string.scheme, R.drawable.scheme_logo);
        createBottomMenuItem(bottomMenu, listener, counter, new OtherFragment(), R.id.button5, R.string.other, R.drawable.other_logo);
        listener.first(get(bottomMenu, R.id.button3, ViewGroup.class));
    }

    private void createBottomMenuItem(LinearLayout menu, BottomMenuClickListener listener, AtomicInteger counter, Fragment f, int itemId, int textId, int imageId) {
        ViewGroup group = get(menu, itemId, ViewGroup.class);
        get(group, R.id.bottom_menu_text, TextView.class).setText(textId);
        get(group, R.id.bottom_menu_image, ImageView.class).setImageResource(imageId);
        if(Build.VERSION.SDK_INT >= 11){
            get(group, R.id.bottom_menu_image, ImageView.class).setAlpha(0.7f);
        }
        group.setTag(BottomMenuClickListener.TAG_FRAGMENT, f);
        group.setTag(BottomMenuClickListener.TAG_IDX, counter.getAndIncrement());
        group.setOnClickListener(listener);
    }

    public void setTitle(String title) {
        setTitle(title);
    }

    public void loadFragmentWithAdd(Fragment f) {
        Log.d("ContentActivity", "loadFragmentWithAdd("+f+")");
        FragmentTransaction transaction = fragmentMgr.beginTransaction();
        transaction.setCustomAnimations(R.anim.abc_fade_in,R.anim.abc_fade_out);
        transaction.replace(R.id.content_frame, f);
        transaction.addToBackStack(null);
        if(listener != null) {
            if (f instanceof MapFragment) {
                listener.onClick(findViewById(R.id.button3));
            }
        }
        transaction.commit();
    }

    private void loadFragmentWithReplace(Fragment f) {
        Log.d("ContentActivity", "loadFragmentWithReplace("+f+")");
        fragmentMgr
            .beginTransaction()
                .setCustomAnimations(R.anim.abc_fade_in, R.anim.abc_fade_out)
                .replace(R.id.content_frame, f)
            .commit();
    }

    private void loadFragmentWithReplaceAnimated(Fragment f, boolean left) {
        Log.d("ContentActivity", "loadFragmentWithReplaceAnim("+f+")");
        int in = R.anim.abc_fade_in; //left ? R.anim.slide_in_left : R.anim.slide_in_right;
        int out = R.anim.abc_fade_out; //left ? R.anim.slide_out_right : R.anim.slide_out_left;

        // TODO Får nullptr här när man byter fragment efter att ha gått ur appen ett tag för att
        // sedan återuppta den  (f är null)
        fragmentMgr
            .beginTransaction()
                .setCustomAnimations(in, out)
                .replace(R.id.content_frame, f)
            .commit();
    }

    public void popFragmentStack() {
        fragmentMgr.popBackStack();
    }

    /**
     * Sets up the ListView in the navigationdrawer menu.
     */
    private void populateRightMenuDrawer() {
        LayoutInflater inflater = LayoutInflater.from(this);

        adapter = new LKRightMenuArrayAdapter(this);
        adapter.addItem(getString(R.string.food),R.drawable.food_logo, new DataType[]{DataType.FOOD});
        adapter.addItem(getString(R.string.fun),R.drawable.fun_logo, new DataType[]{DataType.FUN, DataType.SMALL_FUN, DataType.TENT_FUN, DataType.TOMBOLAN,
                DataType.SCENE, DataType.RADIO});
        adapter.addItem(getString(R.string.help),R.drawable.help_logo, new DataType[]{DataType.POLICE, DataType.CARE});
        adapter.addItem(getString(R.string.wc),R.drawable.wc_logo,new DataType[]{DataType.TOILETS});
        adapter.addItem(getString(R.string.show_all),0, DataType.values());

        rightMenuList.setAdapter(adapter);
        rightMenuList.setOnItemClickListener(adapter);
        rightMenuList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        rightMenuList.setItemsCanFocus(false);
    }

    public void allBottomsUnfocus() {
        listener.deselectItem(getResources());
        listener.selected = null;
    }

    public void openRightMenu(View v){
        if(drawerLayout.isDrawerOpen(Gravity.RIGHT)){
            if (Build.VERSION.SDK_INT > 10) v.setRotation(0);
            drawerLayout.closeDrawer(Gravity.RIGHT);
        } else {
            if (Build.VERSION.SDK_INT > 10) v.setRotation(180);
            drawerLayout.openDrawer(Gravity.RIGHT);
        }
    }

    public void updateMapView(Collection<DataType> types) {
        mapFragment.setActiveType(types);
    }

    private class BottomMenuClickListener implements OnClickListener {
        private static final int TAG_IDX = R.id.bottom_menu_tag_idx;
        private static final int TAG_FRAGMENT = R.id.bottom_menu_tag_fragment;

        private final Resources r = getResources();

        private ViewGroup selected;

        private BottomMenuClickListener() {}

        public void first(ViewGroup first){
            selected = first;
            selectItem(first, r);
        }

        @Override
        public void onClick(View v) {
            if(v == this.selected) return;

            ViewGroup newSelection = (ViewGroup) v;
            Fragment f = (Fragment) v.getTag(TAG_FRAGMENT);

            int newIdx = Integer.class.cast(v.getTag(TAG_IDX));
            if(selected == null) {
                selectItem(newSelection, r);
                loadFragmentWithReplaceAnimated(f, true);
                this.selected = newSelection;
                return;
            }

            int oldIdx = Integer.class.cast(selected.getTag(TAG_IDX));
            boolean moveLeft = (newIdx > oldIdx);

            loadFragmentWithReplaceAnimated(f, moveLeft);

            selectItem(newSelection, r);
            deselectItem(r);

            this.selected = newSelection;
        }

        private void selectItem(ViewGroup selected, Resources res) {
            selected.setBackgroundColor(res.getColor(R.color.bottom_menu_background_selected));
            get(selected, R.id.bottom_menu_text, TextView.class).setTextColor(res.getColor(R.color.white));
            get(selected, R.id.bottom_menu_shadow, LinearLayout.class).setBackgroundColor(res.getColor(R.color.bottom_menu_shadow_selected));
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                get(selected, R.id.bottom_menu_image, ImageView.class).setAlpha(1.0f);
            }
            if(selected.getTag(TAG_FRAGMENT) instanceof MapFragment) {
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
            } else if(selected.getTag(TAG_FRAGMENT) != null) { //When constructing the tag will be null
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
            }
            drawerLayout.closeDrawers();
        }

        private void deselectItem(Resources res) {
            if (selected != null) {
                selected.setBackgroundColor(res.getColor(R.color.red));
                get(selected, R.id.bottom_menu_text, TextView.class).setTextColor(res.getColor(R.color.white_unselected));
                get(selected, R.id.bottom_menu_shadow, LinearLayout.class).setBackgroundColor(res.getColor(R.color.bottom_menu_shadow));
                if(Build.VERSION.SDK_INT >10) {
                    get(selected, R.id.bottom_menu_image, ImageView.class).setAlpha(0.7f);
                }
            }
        }
    }

    private class MusicThread extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            MediaPlayer mp = MediaPlayer.create(ContentActivity.this, R.raw.train_sound);
            mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

                @Override
                public void onCompletion(MediaPlayer mp) {
                    mp.release();
                }

            });
            mp.start();
            Log.d("Train","TRAIN");

            return null;
        }
        //Code goes here
    }
        public void activateTrainButton() {
        ImageButton b = get(mActionBarView, R.id.train, ImageButton.class);
        b.setVisibility(View.VISIBLE);
        b.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {

//                MusicThread thread = new MusicThread();
//                thread.execute();


                loadFragmentWithAdd(TrainMapFragment.create(true));



           }
        });
    }

    public void inactivateTrainButton() {
        get(mActionBarView,R.id.train,ImageButton.class).setVisibility(View.INVISIBLE);
    }



private static class VisibilityOnComplete implements Animation.AnimationListener {
        private final int visibility;
        private final View view;

        private VisibilityOnComplete(View view, int invisible) {
            this.view = view;
            this.visibility = invisible;
        }

        @Override
        public void onAnimationStart(Animation animation) {view.setVisibility(visibility);}

        @Override
        public void onAnimationEnd(Animation animation) {}

        @Override
        public void onAnimationRepeat(Animation animation) {}
    }
}

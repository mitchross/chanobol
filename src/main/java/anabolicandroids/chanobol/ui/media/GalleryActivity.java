package anabolicandroids.chanobol.ui.media;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.transition.TransitionInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.koushikdutta.async.future.FutureCallback;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.List;

import anabolicandroids.chanobol.R;
import anabolicandroids.chanobol.api.data.Post;
import anabolicandroids.chanobol.ui.scaffolding.SwipeRefreshActivity;
import anabolicandroids.chanobol.ui.scaffolding.UiAdapter;
import anabolicandroids.chanobol.util.Util;
import butterknife.InjectView;

public class GalleryActivity extends SwipeRefreshActivity {

    // Construction ////////////////////////////////////////////////////////////////////////////////

    // To immediately load thumbnails without a request to load the respective thread first
    private static String EXTRA_IMAGEPOINTERS = "imagePointers";
    // Needed to form a valid request to 4Chan
    @SuppressWarnings("FieldCanBeLocal")
    private static String EXTRA_BOARDNAME = "boardName";
    @SuppressWarnings("FieldCanBeLocal")
    private static String EXTRA_THREADNUMBER = "threadNumber";

    private ArrayList<MediaPointer> mediaPointers;
    private String boardName;
    private String threadNumber;
    private GalleryAdapter galleryAdapter;

    public static void launch(
            Activity activity,
            String boardName, String threadNumber,
            List<MediaPointer> mediaPointers
    ) {
        ActivityOptionsCompat options = makeSceneTransitionAnimation(activity);
        Intent intent = new Intent(activity, GalleryActivity.class);
        intent.putExtra(EXTRA_BOARDNAME, boardName);
        intent.putExtra(EXTRA_THREADNUMBER, threadNumber);
        intent.putExtra(EXTRA_IMAGEPOINTERS, Parcels.wrap(mediaPointers));
        ActivityCompat.startActivity(activity, intent, options.toBundle());
    }

    @InjectView(R.id.gallery) RecyclerView galleryView;

    @Override protected int getLayoutResource() { return R.layout.activity_gallery; }
    @Override protected RecyclerView getRootRecyclerView() { return galleryView; }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle b = getIntent().getExtras();
        boardName = b.getString("boardName");
        threadNumber = b.getString("threadNumber");
        mediaPointers = Parcels.unwrap(b.getParcelable(EXTRA_IMAGEPOINTERS));

        setTitle(boardName + "/gal/" + threadNumber);

        galleryAdapter = new GalleryAdapter(clickListener, null);
        galleryView.setAdapter(galleryAdapter);
        galleryView.setHasFixedSize(true);
        GridLayoutManager glm = new GridLayoutManager(this, 3);
        galleryView.setLayoutManager(glm);
        galleryView.setItemAnimator(new DefaultItemAnimator());
        Util.calcDynamicSpanCountById(this, galleryView, glm, R.dimen.column_width_gallery);

        if (savedInstanceState == null) load();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // TODO: Ideally the thumbnails should explode in and out but I guess I need to
            // use the begindelayedtransition trick from PostsActivity
            getWindow().setEnterTransition(TransitionInflater.from(this).inflateTransition(android.R.transition.slide_right));
            getWindow().setReturnTransition(TransitionInflater.from(this).inflateTransition(android.R.transition.slide_right));
        }
    }

    // Callbacks ///////////////////////////////////////////////////////////////////////////////////

    View.OnClickListener clickListener = new View.OnClickListener() {
        @Override public void onClick(View v) {
            final GalleryThumbView iv = (GalleryThumbView) v;
            int w = iv.getWidth();
            int h = iv.getHeight();
            final Drawable d = iv.image.getDrawable();
            final int r = Math.min(d.getIntrinsicHeight(), d.getIntrinsicWidth());
            int[] xy = new int[2];
            iv.getLocationOnScreen(xy);
            final int cx = xy[0] + w/2;
            final int cy = xy[1] + h/2;
            iv.postDelayed(new Runnable() {
                @Override public void run() {
                    MediaActivity.transitionBitmap = Util.drawableToBitmap(iv.image.getDrawable());
                    int color = getResources().getColor(R.color.colorPrimaryDark);
                    if (MediaActivity.transitionBitmap != null) {
                        Palette palette = Palette.generate(MediaActivity.transitionBitmap);
                        color = palette.getMutedColor(color);
                    }
                    MediaActivity.launch(
                            GalleryActivity.this, iv.image, iv.index + "", new Point(cx, cy), r, color,
                            true, boardName, threadNumber, iv.index, mediaPointers
                    );
                }
            }, 200);
        }
    };

    // Data Loading ////////////////////////////////////////////////////////////////////////////////

    @Override protected void load() {
        super.load();
        service.listPosts(this, boardName, threadNumber, new FutureCallback<List<Post>>() {
            @Override public void onCompleted(Exception e, List<Post> result) {
                if (e != null) {
                    showToast(e.getMessage());
                    System.out.println("" + e.getMessage());
                    loaded();
                    return;
                }
                mediaPointers.clear();
                for (Post p : result) { if (p.mediaId != null) {
                    mediaPointers.add(new MediaPointer(p, p.mediaId, p.mediaExtension, p.mediaWidth, p.mediaHeight));
                }}
                galleryAdapter.notifyDataSetChanged();
                loaded();
            }
        });
    }

    @Override protected void cancelPending() {
        super.cancelPending();
        // TODO: Reenable once https://github.com/koush/ion/issues/422 is fixed
        //ion.cancelAll(this);
    }

    // Lifecycle ///////////////////////////////////////////////////////////////////////////////////

    @Override public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Util.updateRecyclerViewGridOnConfigChange(galleryView, R.dimen.column_width_gallery);
    }

    // Toolbar Menu ////////////////////////////////////////////////////////////////////////////////

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gallery, menu);
        return true;
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) { }
        return super.onOptionsItemSelected(item);
    }

    // Adapters ////////////////////////////////////////////////////////////////////////////////////

    class GalleryAdapter extends UiAdapter<MediaPointer> {

        public GalleryAdapter(View.OnClickListener clickListener, View.OnLongClickListener longClickListener) {
            super(GalleryActivity.this, clickListener, longClickListener);
            this.items = mediaPointers;
        }

        @Override public View newView(ViewGroup container) {
            return getLayoutInflater().inflate(R.layout.view_gallery_thumb, container, false);
        }

        @Override public void bindView(MediaPointer mediaPointer, int position, View view) {
            GalleryThumbView g = (GalleryThumbView) view;
            g.bindTo(ion, boardName, mediaPointer, position);
            ViewCompat.setTransitionName(g.image, position+"");
        }
    }
}

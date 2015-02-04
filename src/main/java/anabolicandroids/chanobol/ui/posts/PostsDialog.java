package anabolicandroids.chanobol.ui.posts;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ListView;

import anabolicandroids.chanobol.R;
import anabolicandroids.chanobol.api.data.Post;
import anabolicandroids.chanobol.ui.UiFragment;
import butterknife.InjectView;
import butterknife.OnClick;

// TODO: This is bad design, PostsDialog should get a Parcelable list of posts and have it's
// own adapter. Otherwise on recreation adapter is not initialized and will create NPE
public class PostsDialog extends UiFragment {
    // Why not RecyclerView? It _used to be_ RecyclerView but as the by default provided
    // Layoutmanagers do not support wrap_content and thus no easy ability to center the
    // inline posts, it seemed to be the best course of action to simply go back to ListView
    // in the meantime even if it means that PostsFragment.PostsAdapter cannot be reused.
    @InjectView(R.id.posts) ListView postsView;

    public static final String STACK_ID = "postsdialog";

    Post repliedTo;
    Post quotedBy;
    PostsFragment.PostsDialogAdapter adapter;

    @OnClick(R.id.blackness) void dismiss() { getFragmentManager().popBackStack(); }

    @Override
    protected int getLayoutResource() {
        return R.layout.fragment_posts_dialog;
    }

    @Override
    public void onActivityCreated2(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated2(savedInstanceState);
        postsView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onResume() {
        super.onResume();
        adapter.notifyDataSetChanged();
        postsView.requestLayout();
        if (repliedTo != null)
            activity.setTitle(resources.getString(R.string.replies_to) + " " + repliedTo.number);
        else
            activity.setTitle(resources.getString(R.string.quoted_by) + " " + quotedBy.number);
    }

    @Override public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        postsView.postDelayed(new Runnable() {
            @Override public void run() {
                postsView.requestLayout();
            }
        }, 100);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.posts_dialog, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.close:
                getFragmentManager().popBackStack(STACK_ID, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                break;
        }
        return super.onOptionsItemSelected(item);
    }


}

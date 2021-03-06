package com.feeder.android.view.article;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.simplelist.MaterialSimpleListAdapter;
import com.afollestad.materialdialogs.simplelist.MaterialSimpleListItem;
import com.feeder.android.util.AnimationHelper;
import com.feeder.android.util.ArticleUtil;
import com.feeder.android.util.Constants;
import com.feeder.android.util.DateUtil;
import com.feeder.android.util.ShareHelper;
import com.feeder.android.util.StatManager;
import com.feeder.android.view.BaseSwipeActivity;
import com.feeder.common.AppUtil;
import com.feeder.common.SPManager;
import com.feeder.common.ThreadManager;
import com.feeder.domain.ArticleController;
import com.feeder.domain.DBManager;
import com.feeder.model.Article;
import com.feeder.model.ArticleDao;
import com.feeder.model.Subscription;
import com.feeder.model.SubscriptionDao;

import org.sufficientlysecure.htmltextview.HtmlTextView;

import java.util.ArrayList;
import java.util.List;

import cn.sharesdk.framework.ShareSDK;
import me.zsr.feeder.R;

import static com.feeder.android.util.Constants.*;

/**
 * @description:
 * @author: Match
 * @date: 10/23/16
 */

// TODO: 10/30/16 to be modularity
public class ArticleActivity extends BaseSwipeActivity {
    private Toolbar mToolbar;
    private HtmlTextView mContentTextView;
    private TextView mTitleTextView;
    private TextView mSubscriptionNameTextView;
    private TextView mDateTextView;
    private TextView mTimeTextView;
    private ShareHelper mShareHelper;
    private Article mArticle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article);

        initViews();
        Long articleId = getIntent().getExtras().getLong(Constants.KEY_BUNDLE_ARTICLE_ID);
        loadDataAsync(articleId);

        ShareSDK.initSDK(this);
        mShareHelper = new ShareHelper(this);
    }

    private void initViews() {
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mToolbar.inflateMenu(R.menu.menu_article);
        mToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_fav:
                        if (mArticle != null) {
                            if (mArticle.getFavorite()) {
                                mArticle.setFavorite(false);
                                item.setIcon(R.drawable.ic_star_border_white_24dp);
                                Toast.makeText(ArticleActivity.this, R.string.unfavorited, Toast.LENGTH_SHORT).show();
                            } else {
                                mArticle.setFavorite(true);
                                item.setIcon(R.drawable.ic_star_white_24dp);
                                Toast.makeText(ArticleActivity.this, R.string.favorited, Toast.LENGTH_SHORT).show();
                            }
                            ArticleController.getInstance().saveArticle(mArticle);
                        }
                        break;
                    case R.id.action_share:
                        showShareMenu();
                        StatManager.statEvent(ArticleActivity.this, StatManager.EVENT_MENU_SHARE_CLICK);
                        break;
                }
                return false;
            }
        });

        mTitleTextView = (TextView) findViewById(R.id.article_title);
        mDateTextView = (TextView) findViewById(R.id.article_date);
        mTimeTextView = (TextView) findViewById(R.id.article_time);
        mSubscriptionNameTextView = (TextView) findViewById(R.id.subscription_name);

        mContentTextView = (HtmlTextView) findViewById(R.id.article_content);
        switch (SPManager.getInt(KEY_FONT_SIZE, FONT_SIZE_MEDIUM)) {
            case FONT_SIZE_SMALL:
                mContentTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimension(R.dimen.text_size_small));
                break;
            case FONT_SIZE_MEDIUM:
                mContentTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimension(R.dimen.text_size_medium));
                break;
            case FONT_SIZE_BIG:
                mContentTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimension(R.dimen.text_size_big));
                break;
        }
    }

    private void loadDataAsync(final Long articleId) {
        ThreadManager.postInBackground(new Runnable() {
            @Override
            public void run() {
                final List<Article> articleList = DBManager.getArticleDao().queryBuilder().where(
                        ArticleDao.Properties.Id.eq(articleId)).list();
                if (articleList.size() != 1) {
                    return;
                }
                mArticle = articleList.get(0);

                final List<Subscription> subscriptionList = DBManager.getSubscriptionDao().queryBuilder().where(
                        SubscriptionDao.Properties.Id.eq(mArticle.getSubscriptionId())).list();
                if (subscriptionList.size() != 1) {
                    return;
                }

                ThreadManager.post(new Runnable() {
                    @Override
                    public void run() {
                        setData(mArticle, subscriptionList.get(0).getTitle());
                    }
                });
            }
        });
    }

    private void setData(Article article, String subscriptionName) {
        mTitleTextView.setText(article.getTitle());
        mDateTextView.setText(DateUtil.formatDate(this, article.getPublished()));
        mTimeTextView.setText(DateUtil.formatTime(article.getPublished()));
        mSubscriptionNameTextView.setText(subscriptionName);
        ArticleUtil.setContent(this, article, mContentTextView, subscriptionName);
        if (article.getFavorite()) {
            mToolbar.getMenu().findItem(R.id.action_fav).setIcon(R.drawable.ic_star_white_24dp);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        AnimationHelper.overridePendingTransition(this);
    }

    private void showShareMenu() {
        final MaterialSimpleListAdapter adapter = new MaterialSimpleListAdapter(this);
        final List<Integer> contentIdList = new ArrayList<>();
        if (SPManager.getBoolean(KEY_SWITCH_SHARE_WECHAT, true)
                && AppUtil.isAppInstalled(this, Constants.PACKAGE_NAME_WECHAT)) {
            Drawable drawable = getResources().getDrawable(R.drawable.ic_menu_wechat);
            drawable.setColorFilter(getResources().getColor(R.color.main_grey_normal), PorterDuff.Mode.SRC_IN);
            adapter.add(new MaterialSimpleListItem.Builder(this)
                    .content(R.string.wechat)
                    .icon(drawable)
                    .backgroundColor(Color.WHITE)
                    .build());
            contentIdList.add(R.string.wechat);
        }
        if (SPManager.getBoolean(KEY_SWITCH_SHARE_MOMENT, true)
                && AppUtil.isAppInstalled(this, Constants.PACKAGE_NAME_WECHAT)) {
            Drawable drawable = getResources().getDrawable(R.drawable.ic_menu_moment);
            drawable.setColorFilter(getResources().getColor(R.color.main_grey_normal), PorterDuff.Mode.SRC_IN);
            adapter.add(new MaterialSimpleListItem.Builder(this)
                    .content(R.string.moment)
                    .icon(drawable)
                    .backgroundColor(Color.WHITE)
                    .build());
            contentIdList.add(R.string.moment);
        }
        if (SPManager.getBoolean(KEY_SWITCH_SHARE_WEIBO, true)
                && AppUtil.isAppInstalled(this, Constants.PACKAGE_NAME_WEIBO)) {
            Drawable drawable = getResources().getDrawable(R.drawable.ic_menu_weibo);
            drawable.setColorFilter(getResources().getColor(R.color.main_grey_normal), PorterDuff.Mode.SRC_IN);
            adapter.add(new MaterialSimpleListItem.Builder(this)
                    .content(R.string.weibo)
                    .icon(drawable)
                    .backgroundColor(Color.WHITE)
                    .build());
            contentIdList.add(R.string.weibo);
        }
        if (SPManager.getBoolean(KEY_SWITCH_SHARE_INSTAPAPER, true)
                && AppUtil.isAppInstalled(this, Constants.PACKAGE_NAME_INSTAPAPER)) {
            Drawable drawable = getResources().getDrawable(R.drawable.ic_menu_instapaper);
            drawable.setColorFilter(getResources().getColor(R.color.main_grey_normal), PorterDuff.Mode.SRC_IN);
            adapter.add(new MaterialSimpleListItem.Builder(this)
                    .content(R.string.instapaper)
                    .icon(drawable)
                    .backgroundColor(Color.WHITE)
                    .build());
            contentIdList.add(R.string.instapaper);
        }
        if (SPManager.getBoolean(KEY_SWITCH_SHARE_POCKET, true)
                && AppUtil.isAppInstalled(this, Constants.PACKAGE_NAME_POCKET)) {
            Drawable drawable = getResources().getDrawable(R.drawable.ic_menu_pocket);
            drawable.setColorFilter(getResources().getColor(R.color.main_grey_normal), PorterDuff.Mode.SRC_IN);
            adapter.add(new MaterialSimpleListItem.Builder(this)
                    .content(R.string.pocket)
                    .icon(drawable)
                    .backgroundColor(Color.WHITE)
                    .build());
            contentIdList.add(R.string.pocket);
        }
        if (SPManager.getBoolean(KEY_SWITCH_SHARE_EVERNOTE, true)
                && AppUtil.isAppInstalled(this, Constants.PACKAGE_NAME_EVERNOTE)) {
            Drawable drawable = getResources().getDrawable(R.drawable.ic_menu_evernote);
            drawable.setColorFilter(getResources().getColor(R.color.main_grey_normal), PorterDuff.Mode.SRC_IN);
            adapter.add(new MaterialSimpleListItem.Builder(this)
                    .content(R.string.evernote)
                    .icon(drawable)
                    .backgroundColor(Color.WHITE)
                    .build());
            contentIdList.add(R.string.evernote);
        }

        if (SPManager.getBoolean(KEY_SWITCH_SHARE_MORE, true)) {
            Drawable drawable = getResources().getDrawable(R.drawable.ic_menu_more);
            drawable.setColorFilter(getResources().getColor(R.color.main_grey_normal), PorterDuff.Mode.SRC_IN);
            adapter.add(new MaterialSimpleListItem.Builder(this)
                    .content(R.string.more)
                    .icon(drawable)
                    .backgroundColor(Color.WHITE)
                    .build());
            contentIdList.add(R.string.more);
        }

        if (contentIdList.size() == 0 ||
                (contentIdList.size() == 1 &&contentIdList.get(0) == R.string.more)) {
            mShareHelper.shareToOthers(mArticle);
            StatManager.statEvent(ArticleActivity.this, StatManager.EVENT_SHARE_ITEM_CLICK, StatManager.TAG_SHARE_OTHERS);
            return;
        }

        new MaterialDialog.Builder(this)
                .title(R.string.share_to)
                .adapter(adapter, new MaterialDialog.ListCallback() {
                    @Override
                    public void onSelection(MaterialDialog dialog, View itemView, int which, CharSequence text) {
                        switch (contentIdList.get(which)) {
                            case R.string.wechat:
                                mShareHelper.shareToWechat(mArticle);
                                StatManager.statEvent(ArticleActivity.this, StatManager.EVENT_SHARE_ITEM_CLICK, StatManager.TAG_SHARE_WECHAT);
                                break;
                            case R.string.moment:
                                mShareHelper.shareToMoment(mArticle);
                                StatManager.statEvent(ArticleActivity.this, StatManager.EVENT_SHARE_ITEM_CLICK, StatManager.TAG_SHARE_MOMENT);
                                break;
                            case R.string.weibo:
                                mShareHelper.shareToWeibo(mArticle);
                                StatManager.statEvent(ArticleActivity.this, StatManager.EVENT_SHARE_ITEM_CLICK, StatManager.TAG_SHARE_WEIBO);
                                break;
                            case R.string.instapaper:
                                mShareHelper.shareToApp(mArticle, Constants.PACKAGE_NAME_INSTAPAPER);
                                StatManager.statEvent(ArticleActivity.this, StatManager.EVENT_SHARE_ITEM_CLICK, StatManager.TAG_SHARE_INSTAPAPER);
                                break;
                            case R.string.pocket:
                                mShareHelper.shareToApp(mArticle, Constants.PACKAGE_NAME_POCKET);
                                StatManager.statEvent(ArticleActivity.this, StatManager.EVENT_SHARE_ITEM_CLICK, StatManager.TAG_SHARE_POCKET);
                                break;
                            case R.string.evernote:
                                mShareHelper.shareToApp(mArticle, Constants.PACKAGE_NAME_EVERNOTE);
                                StatManager.statEvent(ArticleActivity.this, StatManager.EVENT_SHARE_ITEM_CLICK, StatManager.TAG_SHARE_EVERNOTE);
                                break;
                            case R.string.more:
                                mShareHelper.shareToOthers(mArticle);
                                StatManager.statEvent(ArticleActivity.this, StatManager.EVENT_SHARE_ITEM_CLICK, StatManager.TAG_SHARE_OTHERS);
                                break;

                        }
                        dialog.dismiss();
                    }
                })
                .show();
    }
}

package com.thinkbiganalytics.metadata.modeshape.feed;

import com.thinkbiganalytics.metadata.api.category.Category;
import com.thinkbiganalytics.metadata.api.feed.Feed;
import com.thinkbiganalytics.metadata.api.feed.FeedProvider;
import com.thinkbiganalytics.metadata.api.feedmgr.category.FeedManagerCategory;
import com.thinkbiganalytics.metadata.api.feedmgr.feed.FeedManagerFeed;
import com.thinkbiganalytics.metadata.api.feedmgr.feed.FeedManagerFeedProvider;
import com.thinkbiganalytics.metadata.api.feedmgr.template.FeedManagerTemplate;
import com.thinkbiganalytics.metadata.modeshape.BaseJcrProvider;
import com.thinkbiganalytics.metadata.modeshape.category.JcrCategory;
import com.thinkbiganalytics.metadata.modeshape.common.EntityUtil;
import com.thinkbiganalytics.metadata.modeshape.common.JcrEntity;
import com.thinkbiganalytics.metadata.modeshape.support.JcrUtil;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

/**
 * Created by sr186054 on 6/8/16.
 */


public class JcrFeedManagerFeedProvider extends BaseJcrProvider<FeedManagerFeed, Feed.ID> implements FeedManagerFeedProvider {

    @Override
    public String getNodeType() {
        return JcrFeed.NODE_TYPE;
    }


    @Override
    public Class<JcrFeedManagerFeed> getEntityClass() {
        return JcrFeedManagerFeed.class;
    }

    @Override
    public Class<? extends JcrEntity> getJcrEntityClass() {
        return JcrFeedManagerFeed.class;
    }

    @Inject
    private FeedProvider feedProvider;


    @Override
    public FeedManagerFeed findBySystemName(String categorySystemName, String systemName) {
        Feed feed = feedProvider.findBySystemName(categorySystemName, systemName);
        if (feed != null) {
            return (FeedManagerFeed) feed;
        }
        return null;
    }

    public FeedManagerFeed ensureFeed(Feed feed) {
        FeedManagerFeed fmFeed = findById(feed.getId());
        if (fmFeed == null) {
            JcrFeed jcrFeed = (JcrFeed) feed;
            fmFeed = new JcrFeedManagerFeed(jcrFeed.getNode());
        }
        return fmFeed;
    }

    public FeedManagerFeed ensureFeed(Category.ID categoryId, String feedSystemName) {
        Feed feed = feedProvider.ensureFeed(categoryId, feedSystemName);
        return ensureFeed(feed);
    }



    @Override
    //TODO FIX SQL
    public List<? extends FeedManagerFeed> findByTemplateId(FeedManagerTemplate.ID templateId) {
        String query = "SELECT * from " + EntityUtil.asQueryProperty(JcrFeed.NODE_TYPE) + " as e where e." + EntityUtil.asQueryProperty(JcrFeedManagerFeed.TEMPLATE) + ".id = $id";
        Map<String, String> bindParams = new HashMap<>();
        bindParams.put("id", templateId.toString());
        return JcrUtil.find(getSession(), query, JcrFeedManagerFeed.class);
    }

    @Override
    //TODO FIX SQL
    public List<? extends FeedManagerFeed> findByCategoryId(FeedManagerCategory.ID categoryId) {

        String query = "SELECT * from " + EntityUtil.asQueryProperty(JcrFeed.NODE_TYPE) + " as e "
                       + "INNER JOIN [" + JcrCategory.NODE_TYPE + "] as c ON ISSAMENODE(e." + EntityUtil.asQueryProperty(JcrFeedManagerFeed.CATEGORY) + ", c)";

        Map<String, String> bindParams = new HashMap<>();
        bindParams.put("id", categoryId.toString());
        return JcrUtil.find(getSession(), query, JcrFeedManagerFeed.class);
    }

    public Feed.ID resolveId(Serializable fid) {
        return new JcrFeed.FeedId(fid);
    }

}

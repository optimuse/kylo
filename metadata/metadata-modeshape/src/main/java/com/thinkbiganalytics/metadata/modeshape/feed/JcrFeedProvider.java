/**
 *
 */
package com.thinkbiganalytics.metadata.modeshape.feed;

import com.google.common.base.Predicate;
import com.thinkbiganalytics.metadata.api.category.Category;
import com.thinkbiganalytics.metadata.api.category.CategoryProvider;
import com.thinkbiganalytics.metadata.api.datasource.Datasource;
import com.thinkbiganalytics.metadata.api.datasource.DatasourceProvider;
import com.thinkbiganalytics.metadata.api.feed.Feed;
import com.thinkbiganalytics.metadata.api.feed.Feed.ID;
import com.thinkbiganalytics.metadata.api.feed.FeedCriteria;
import com.thinkbiganalytics.metadata.api.feed.FeedDestination;
import com.thinkbiganalytics.metadata.api.feed.FeedProvider;
import com.thinkbiganalytics.metadata.api.feed.FeedSource;
import com.thinkbiganalytics.metadata.modeshape.AbstractMetadataCriteria;
import com.thinkbiganalytics.metadata.modeshape.BaseJcrProvider;
import com.thinkbiganalytics.metadata.modeshape.MetadataRepositoryException;
import com.thinkbiganalytics.metadata.modeshape.category.JcrCategory;
import com.thinkbiganalytics.metadata.modeshape.common.EntityUtil;
import com.thinkbiganalytics.metadata.modeshape.common.JcrEntity;
import com.thinkbiganalytics.metadata.modeshape.datasource.JcrDatasource;
import com.thinkbiganalytics.metadata.modeshape.datasource.JcrDestination;
import com.thinkbiganalytics.metadata.modeshape.datasource.JcrSource;
import com.thinkbiganalytics.metadata.modeshape.support.JcrUtil;
import com.thinkbiganalytics.metadata.sla.api.Metric;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 *
 * @author Sean Felten
 */
public class JcrFeedProvider extends BaseJcrProvider<Feed, Feed.ID> implements FeedProvider {

    @Inject
    CategoryProvider<Category> categoryProvider;

    @Inject
    DatasourceProvider datasourceProvider;

    @Override
    public String getNodeType() {
        return JcrFeed.NODE_TYPE;
    }


    @Override
    public Class<JcrFeed> getEntityClass() {
        return JcrFeed.class;
    }

    @Override
    public Class<? extends JcrEntity> getJcrEntityClass() {
        return JcrFeed.class;
    }


    /**
     *
     */
    public JcrFeedProvider() {
        // TODO Auto-generated constructor stub
    }

    @Override
    public FeedSource ensureFeedSource(ID feedId, com.thinkbiganalytics.metadata.api.datasource.Datasource.ID dsId) {
        JcrFeed feed = (JcrFeed) findById(feedId);
        FeedSource source = feed.getSource(dsId);
        if (source == null) {
            JcrDatasource datasource = (JcrDatasource) datasourceProvider.getDatasource(dsId);
            try {
                if (datasource != null) {
                    Node feedNode = getNodeByIdentifier(feedId);
                    //    JcrUtil.getOrCreateNode(feedNode,JcrFeed.SOURCE_NAME,JcrSource.NODE_TYPE,JcrSource.class,new Object[] {datasource});
                    Node feedSourceNode = feedNode.addNode(JcrFeed.SOURCE_NAME, JcrSource.NODE_TYPE);
                    JcrSource jcrSource = new JcrSource(feedSourceNode, datasource);
                    return jcrSource;
                }
            } catch (RepositoryException e) {
                throw new MetadataRepositoryException("Unable to create feedSource for dataSource " + dsId + " with Feed Id of " + feedId, e);
            }
        }
        return source;
    }

    @Override
    public FeedSource ensureFeedSource(ID feedId, com.thinkbiganalytics.metadata.api.datasource.Datasource.ID id, com.thinkbiganalytics.metadata.sla.api.ServiceLevelAgreement.ID slaId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public FeedDestination ensureFeedDestination(ID feedId, com.thinkbiganalytics.metadata.api.datasource.Datasource.ID dsId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Feed ensureFeed(Category.ID categoryId, String feedSystemName) {
        Category category = categoryProvider.findById(categoryId);
        return ensureFeed(category.getName(),feedSystemName);
    }

    @Override
    public Feed ensureFeed(String categorySystemName, String feedSystemName) {
        String categoryPath = EntityUtil.pathForCategory(categorySystemName);
        JcrCategory category = (JcrCategory) categoryProvider.findBySystemName(categorySystemName);
        Node feedNode = findOrCreateEntityNode(categoryPath, feedSystemName);

        JcrFeed feed = new JcrFeed(feedNode, category);

        feed.setSystemName(feedSystemName);
        return feed;
    }

    @Override
    public Feed ensureFeed(String categorySystemName, String feedSystemName, String descr) {
        Feed feed = ensureFeed(categorySystemName,feedSystemName);
        feed.setDescription(descr);
        return feed;
    }

    @Override
    public Feed ensureFeed(String categorySystemName, String feedSystemName, String descr, Datasource.ID destId) {
        Feed feed = ensureFeed(categorySystemName,feedSystemName,descr);
        //TODO add/find datasources
        return feed;
    }

    @Override
    public Feed ensureFeed(String categorySystemName, String feedSystemName, String descr, Datasource.ID srcId, Datasource.ID destId) {
        Feed feed = ensureFeed(categorySystemName,feedSystemName,descr);
        if (srcId != null) {
            ensureFeedSource(feed.getId(), srcId);
        }
        return feed;
    }

    @Override
    public Feed ensurePrecondition(ID feedId,  String name, String descr, List<List<Metric>> metrics) {
        return null;
    }

    @Override
    public Feed updatePrecondition(ID feedId, List<List<Metric>> metrics) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public FeedCriteria feedCriteria() {
        return new Criteria();
    }

    @Override
    public Feed getFeed(ID id) {
        return findById(id);
    }

    @Override
    public List<Feed> getFeeds() {
        return findAll();
    }

    @Override
    public List<Feed> getFeeds(FeedCriteria criteria) {

        if (criteria != null) {
            Criteria criteriaImpl = (Criteria) criteria;
            return criteriaImpl.select(getSession(), JcrFeed.NODE_TYPE, Feed.class, JcrFeed.class);
        }
        return null;
    }

    @Override
    public Feed findBySystemName(String categorySystemName, String systemName) {
        FeedCriteria c = feedCriteria();
        c.category(categorySystemName);
        c.name(systemName);
        List<Feed> feeds = getFeeds(c);
        if (feeds != null && !feeds.isEmpty()) {
            return feeds.get(0);
        }
        return null;
    }

    @Override
    public FeedSource getFeedSource(com.thinkbiganalytics.metadata.api.feed.FeedSource.ID id) {
        try {
            Node node = getSession().getNodeByIdentifier(id.toString());

            if (node != null) {
                return JcrUtil.createJcrObject(node, JcrSource.class);
            }
        } catch (RepositoryException e) {
            throw new MetadataRepositoryException("Unable to get Feed Destination for " + id);
        }
        return null;
    }

    @Override
    public FeedDestination getFeedDestination(com.thinkbiganalytics.metadata.api.feed.FeedDestination.ID id) {
        try {
            Node node = getSession().getNodeByIdentifier(id.toString());

            if (node != null) {
                return JcrUtil.createJcrObject(node, JcrDestination.class);
            }
        } catch (RepositoryException e) {
            throw new MetadataRepositoryException("Unable to get Feed Destination for " + id);
        }
        return null;

    }

    @Override
    public ID resolveFeed(Serializable fid) {
        return resolveId(fid);
    }

    @Override
    public com.thinkbiganalytics.metadata.api.feed.FeedSource.ID resolveSource(Serializable sid) {
        return new JcrSource.FeedSourceId((sid));
    }

    @Override
    public com.thinkbiganalytics.metadata.api.feed.FeedDestination.ID resolveDestination(Serializable sid) {
        return new JcrDestination.FeedDestinationId(sid);
    }

    @Override
    public boolean enableFeed(ID id) {

        Feed feed = getFeed(id);
        if (!feed.getState().equals(Feed.State.ENABLED)) {
            feed.setState(Feed.State.ENABLED);
            return true;
        }
        return false;

    }

    @Override
    public boolean disableFeed(ID id) {
        Feed feed = getFeed(id);
        if (!feed.getState().equals(Feed.State.DISABLED)) {
            feed.setState(Feed.State.DISABLED);
            return true;
        }
        return false;
    }


    private static class Criteria extends AbstractMetadataCriteria<FeedCriteria> implements FeedCriteria, Predicate<Feed> {

        private String name;
        private Set<Datasource.ID> sourceIds = new HashSet<>();
        private Set<Datasource.ID> destIds = new HashSet<>();
        private String category;

        @Override
        protected void applyFilter(StringBuilder queryStr, HashMap<String, Object> params) {
            StringBuilder cond = new StringBuilder();
            StringBuilder join = new StringBuilder();

            if (this.name != null) {
                cond.append(EntityUtil.asQueryProperty(JcrFeed.SYSTEM_NAME) + " = $name");
                params.put("name", this.name);
            }
            if (this.category != null) {
                //TODO FIX SQL
                join.append(
                    " join [" + JcrCategory.NODE_TYPE + "] c on e." + EntityUtil.asQueryProperty(JcrFeed.CATEGORY) + "." + EntityUtil.asQueryProperty(JcrCategory.SYSTEM_NAME) + " = c." + EntityUtil
                        .asQueryProperty(JcrCategory.SYSTEM_NAME));
                cond.append(" c." + EntityUtil.asQueryProperty(JcrCategory.SYSTEM_NAME) + " = $category ");
                params.put("category", this.category);
            }

            applyIdFilter(cond, join, this.sourceIds, "sources", params);
            applyIdFilter(cond, join, this.destIds, "destinations", params);

            if (join.length() > 0) {
                queryStr.append(join.toString());
            }

            if (cond.length() > 0) {
                queryStr.append(" where ").append(cond.toString());
            }
        }

        private void applyIdFilter(StringBuilder cond, StringBuilder join, Set<Datasource.ID> idSet, String relation, HashMap<String, Object> params) {
            if (!idSet.isEmpty()) {
                if (cond.length() > 0) {
                    cond.append("and ");
                }

                String alias = relation.substring(0, 1);
                join.append("join e.").append(relation).append(" ").append(alias).append(" ");
                cond.append(alias).append(".datasource.id in $").append(relation).append(" ");
                params.put(relation, idSet);
            }
        }

        @Override
        public boolean apply(Feed input) {
            if (this.name != null && !name.equals(input.getName())) {
                return false;
            }
            if (this.category != null && input.getCategory() != null && !this.category.equals(input.getCategory().getName())) {
                return false;
            }
            if (!this.destIds.isEmpty()) {
                List<FeedDestination> destinations = input.getDestinations();
                for (FeedDestination dest : destinations) {
                    if (this.destIds.contains(dest.getDatasource().getId())) {
                        return true;
                    }
                }
                return false;
            }

            if (!this.sourceIds.isEmpty()) {
                List<FeedSource> sources = input.getSources();
                for (FeedSource src : sources) {
                    if (this.sourceIds.contains(src.getDatasource().getId())) {
                        return true;
                    }
                }
                return false;
            }

            return true;
        }

        @Override
        public FeedCriteria sourceDatasource(Datasource.ID id, Datasource.ID... others) {
            this.sourceIds.add(id);
            for (Datasource.ID other : others) {
                this.sourceIds.add(other);
            }
            return this;
        }

        @Override
        public FeedCriteria destinationDatasource(Datasource.ID id, Datasource.ID... others) {
            this.destIds.add(id);
            for (Datasource.ID other : others) {
                this.destIds.add(other);
            }
            return this;
        }

        @Override
        public FeedCriteria name(String name) {
            this.name = name;
            return this;
        }

        @Override
        public FeedCriteria category(String category) {
            this.category = category;
            return this;
        }
    }

    public Feed.ID resolveId(Serializable fid) {
        return new JcrFeed.FeedId(fid);
    }

}

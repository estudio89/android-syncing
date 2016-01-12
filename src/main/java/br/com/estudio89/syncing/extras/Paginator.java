package br.com.estudio89.syncing.extras;

import com.orm.SugarRecord;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by luccascorrea on 3/25/15.
 */
public class Paginator {
    public static abstract class PaginatorProvider {
        /**
         * When called, should fetch more items from the server.
         * @param params
         */
        public abstract void fetchMoreFromServer(Object... params);

        /**
         * Must return a boolean indicating if there are more items in the server.
         * @param params
         * @return
         */
        public abstract boolean moreOnServer(Object... params);

        /**
         * Must return the "order_by" clause used when fetching data
         * from cache.
         *
         * @return
         */
        public abstract String getOrderBy();

        /**
         * Should return a string with a "where" clause to be used
         * when fetching objects from cache.
         *
         * Note that this should be a complete clause, meaning
         * every substitution must have already been made.
         *
         * @param params
         * @return
         */
        protected String getWhereClause(Object... params) {
            return "";
        }
    }

    private Class klass;
    private PaginatorProvider provider;
    private long pageSize;

    public Paginator(Class klass, PaginatorProvider provider, long pageSize) {
        this.klass = klass;
        this.provider = provider;
        this.pageSize = pageSize;

    }

    public boolean canLoadMore(List loadedObjects, Object... params) {
        if (this.provider.moreOnServer(params)) {
            return true;
        } else {
            long count = SugarRecord.count(this.klass, this.provider.getWhereClause(params), new String[]{});
            return count > loadedObjects.size();
        }
    }

    public List loadMore(Object... params) {
        return this.loadMore(new ArrayList(), params);
    }

    public List loadMore(List loadedObjects, Object... params) {
        long count = SugarRecord.count(this.klass,this.provider.getWhereClause(params),new String[]{});
        if (loadedObjects.size() == count) {
            this.provider.fetchMoreFromServer(params);
            return new ArrayList();
        } else {
            return SugarRecord.find(this.klass,this.provider.getWhereClause(params),new String[]{},"",this.provider.getOrderBy(),"" + loadedObjects.size() + ", " + this.pageSize);
        }
    }
}


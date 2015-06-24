package br.com.estudio89.syncing.manager;

import br.com.estudio89.syncing.models.SyncModel;
import br.com.estudio89.syncing.serialization.annotations.JSON;
import br.com.estudio89.syncing.serialization.annotations.NestedManager;
import com.orm.dsl.Ignore;

import java.util.Date;
import java.util.List;

/**
 * Created by luccascorrea on 6/21/15.
 */
public class TestSyncModel extends SyncModel<TestSyncModel> {
    Date pubDate;
    Date creationDate;

    String name;

    @JSON(name="parent_id")
    ParentSyncModel parent;

    @Ignore
    @NestedManager(manager=ChildSyncManager.class)
    @JSON(name="children_objs")
    List<ChildSyncModel> children;

    public Date getPubDate() {
        return pubDate;
    }

    public void setPubDate(Date pubDate) {
        this.pubDate = pubDate;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ParentSyncModel getParent() {
        return parent;
    }

    public void setParent(ParentSyncModel parent) {
        this.parent = parent;
    }

    public List<ChildSyncModel> getChildren() {
        return children;
    }

    public void setChildren(List<ChildSyncModel> children) {
        this.children = children;
    }
}

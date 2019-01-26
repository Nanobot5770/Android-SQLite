package de.thomasdreja.tools.sqlitestoreable;

import java.util.Collection;

public interface StoredCollection<N extends StoredCollection.CollectionNode> extends StoreAble, Collection<N> {

    N getNode(long nodeId);

    interface CollectionNode extends StoreAble {
        long getParentId();
        void setParentId(long id);
    }
}

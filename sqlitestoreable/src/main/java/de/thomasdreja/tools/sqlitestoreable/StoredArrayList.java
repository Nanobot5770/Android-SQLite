package de.thomasdreja.tools.sqlitestoreable;

import java.util.ArrayList;
import java.util.Collection;

/**
 * An extension of the ArrayList that allows for each of the lists elements to be saved into the database
 * @param <N> StoreAble Node element contained in this list
 */
public abstract class StoredArrayList<N extends StoredCollection.CollectionNode> extends ArrayList<N> implements StoredCollection<N> {

    /**
     * ID of the list
     */
    protected long id;

    /**
     * Creates a new, empty list
     */
    protected StoredArrayList() {
        super();
        id = -1;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public void setId(long id) {
        this.id = id;
    }

    @Override
    public N getNode(long nodeId) {
        for(N node : this) {
            if(node.getId() == nodeId) {
                return node;
            }
        }
        return null;
    }

    @Override
    public boolean add(N n) {
        n.setParentId(id);
        return super.add(n);
    }

    @Override
    public void add(int index, N element) {
        element.setParentId(id);
        super.add(index, element);
    }

    @Override
    public boolean addAll(Collection<? extends N> c) {
        for(N node : c) {
            node.setId(id);
        }
        return super.addAll(c);
    }

    @Override
    public boolean addAll(int index, Collection<? extends N> c) {
        for(N node : c) {
            node.setId(id);
        }
        return super.addAll(index, c);
    }

    @Override
    public N remove(int index) {
        N node = super.remove(index);
        node.setParentId(-1);
        return node;
    }

    @Override
    public N set(int index, N element) {
        element.setParentId(id);
        return super.set(index, element);
    }
}

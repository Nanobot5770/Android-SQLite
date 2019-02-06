package de.thomasdreja.tools.sqlitestoreable.impl;

import java.util.ArrayList;
import java.util.Collection;

import de.thomasdreja.tools.sqlitestoreable.template.StoreAble;
import de.thomasdreja.tools.sqlitestoreable.template.StoreAbleCollection;

/**
 * This class extends the default ArrayList and allows this list to be stored within the database.
 * @param <S> Class of the stored child objects
 */
public abstract class ArrayListStoreAble<S extends StoreAble> extends ArrayList<S> implements StoreAbleCollection<S> {

    /**
     * Class of the stored child objects
     */
    private final Class<S> storedClass;

    /**
     * ID of the StoreAble, invalid id if not yet added to database
     * @see StoreAble#INVALID_ID
     */
    long id;

    /**
     * ID of a parent StoreAble, invalid id if none exists
     * @see StoreAble#INVALID_ID
     */
    long relatedId;

    /**
     * Creates a new, empty list that children can be attached to
     * @param storedClass Class of the stored child objects
     */
    @SuppressWarnings("WeakerAccess")
    protected ArrayListStoreAble(Class<S> storedClass) {
        super();
        this.storedClass = storedClass;
        this.id = INVALID_ID;
        this.relatedId = INVALID_ID;
    }

    @Override
    public void setCollection(Collection<S> relatedElements) {
        this.clear();
        this.addAll(relatedElements);
    }

    @Override
    public Collection<S> getCollection() {
        return this;
    }

    @Override
    public Class<S> getChildClass() {
        return storedClass;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public void setId(long id) {
        this.id = id;
        for(S child : this) {
            child.setRelatedId(id);
        }
    }

    @Override
    public long getRelatedId() {
        return relatedId;
    }

    @Override
    public void setRelatedId(long id) {
        this.relatedId = id;
    }

    @Override
    public void add(int index, S child) {
        child.setRelatedId(id);
        super.add(index, child);
    }

    @Override
    public boolean add(S child) {
        child.setRelatedId(id);
        return super.add(child);
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public boolean addAll(Collection<? extends S> collection) {
        for(S child : collection) {
            child.setRelatedId(id);
        }
        return super.addAll(collection);
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public boolean addAll(int index, Collection<? extends S> collection) {
        for(S child : collection) {
            child.setRelatedId(id);
        }
        return super.addAll(index, collection);
    }

    @Override
    public boolean remove(Object child) {
        if(child instanceof StoreAble) {
            ((StoreAble) child).setRelatedId(INVALID_ID);
        }
        return super.remove(child);
    }

    @Override
    public S remove(int index) {
        S child = super.remove(index);
        if(child != null) {
            child.setRelatedId(INVALID_ID);
        }
        return child;
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public boolean removeAll(Collection<?> collection) {
        for(Object child : collection) {
            if(child instanceof StoreAble) {
                ((StoreAble) child).setRelatedId(INVALID_ID);
            }
        }
        return super.removeAll(collection);
    }
}

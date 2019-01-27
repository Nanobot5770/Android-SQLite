package de.thomasdreja.tools.sqlitestoreable.template;

import java.util.Collection;

/**
 * This interface extends the default StoreAble implementation and attaches all related StoreAbles as a collection child-objects of this class
 * @param <S> Class of related StoreAbles stored within
 */
public interface StoreAbleCollection<S extends StoreAble> extends StoreAble {

    /**
     * Replaces the current collection of child-objects with the given one
     * @param relatedElements Collection of child objects
     */
    void setCollection(Collection<S>  relatedElements);

    /**
     * Returns the current collection of child-objects attached to this StoreAble
     * @return Collection of child objects
     */
    Collection<S> getCollection();

    /**
     * Returns the class of child-objects attached to this StoreAble
     * @return Class of Child-Objects, must implement StoreAble
     */
    Class<S> getChildClass();
}

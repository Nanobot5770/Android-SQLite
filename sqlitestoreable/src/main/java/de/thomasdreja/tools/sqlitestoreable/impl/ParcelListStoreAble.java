package de.thomasdreja.tools.sqlitestoreable.impl;

import android.os.Parcel;
import android.os.Parcelable;

import de.thomasdreja.tools.sqlitestoreable.reflection.TableInformation;
import de.thomasdreja.tools.sqlitestoreable.template.StoreAble;

/**
 * This abstract class extends the default ArrayListStoreAble to allow it to be sent via Parcels in Android. Child objects therefore must also be Parcelable
 * @param <P> Class of the child objects, must both implement StoreAble and Parcelable
 */
public abstract class ParcelListStoreAble<P extends StoreAble & Parcelable> extends ArrayListStoreAble<P> implements Parcelable {

    /**
     * Creates a new empty list for the given child object class
     * @param childClass Class of the child objects
     * @see ArrayListStoreAble#ArrayListStoreAble(Class)
     */
    public ParcelListStoreAble(Class<P> childClass) {
        super(childClass);
    }

    /**
     * Unpacks the parcel and fills a new list with the stored child obbjects. Use this constructor for a Parcelable implementation.
     * @param childClass Class of the child objects
     * @param in Parcel that contains a packed ParcelListStoreAble
     */
    protected ParcelListStoreAble(Class<P> childClass, Parcel in) {
        super(childClass);

        setId(in.readLong());
        setRelatedId(in.readLong());

        final int size = in.readInt();

        for(int i=0; i < size; i++) {
            this.add(getChildClass().cast(in.readParcelable(getChildClass().getClassLoader())));
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeLong(relatedId);

        dest.writeInt(size());
        for(P child : this) {
            dest.writeParcelable(child, flags);
        }
    }
}

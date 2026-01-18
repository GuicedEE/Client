package com.guicedee.client.services;

import java.util.Comparator;
/**
 * Provides default ordering for service implementations.
 *
 * @param <J> the service type
 */
public interface IDefaultService<J extends IDefaultService<J>>
        extends Comparable<J>, Comparator<J> {

    /**
     * Compares two services by {@link #sortOrder()}.
     *
     * @param o1 the first service
     * @param o2 the second service
     * @return the comparison result
     */
    @Override
    default int compare(J o1, J o2) {
        if (o1 == null || o2 == null) {
            return -1;
        }
        return o1.sortOrder()
                .compareTo(o2.sortOrder());
    }

    /**
     * Returns the service sort order; lower values execute earlier.
     *
     * @return the sort order (default 100)
     */
    default Integer sortOrder() {
        return 100;
    }

    /**
     * Compares this service to another by {@link #sortOrder()}.
     *
     * @param o the other service
     * @return the comparison result
     */
    @Override
    default int compareTo( J o) {
        int sort = sortOrder().compareTo(o.sortOrder());
        if (sort == 0) {
            return -1;
        }
        return sort;
    }


}

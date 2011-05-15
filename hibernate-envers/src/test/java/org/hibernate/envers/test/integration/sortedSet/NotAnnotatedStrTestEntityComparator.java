package org.hibernate.envers.test.integration.sortedSet;

import java.util.Comparator;

public class NotAnnotatedStrTestEntityComparator implements Comparator<NotAnnotatedStrTestEntity> {
    public static final NotAnnotatedStrTestEntityComparator INSTANCE = new NotAnnotatedStrTestEntityComparator();

    @Override
    public int compare(NotAnnotatedStrTestEntity o1, NotAnnotatedStrTestEntity o2) {
        return o1.getStr().compareTo(o2.getStr());
    }
}

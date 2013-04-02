package org.hibernate.ejb.criteria.jpaMapMode;

import java.util.Date;
import java.util.UUID;

/**
 * A set of logical types of properties. These will be mapped to column types by the hibernate mapping.
 */
public enum PropertyType {
    ID, DATE, DATETIME, LONG, DOUBLE, TEXT, MULTILINE_TEXT, CHOICE,;

    public Class<?> getJavaTypeForPropertyType() {
        switch (this) {
            case DATE:
            case DATETIME:
                return Date.class;

            case LONG:
                return Long.class;

            case DOUBLE:
                return Double.class;

            case TEXT:
            case MULTILINE_TEXT:
                return String.class;

            case ID:
                return UUID.class;
        }

        throw new IllegalArgumentException("Unknown property type: " + this);
    }
}

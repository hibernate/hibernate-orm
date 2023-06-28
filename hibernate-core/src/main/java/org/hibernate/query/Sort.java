package org.hibernate.query;

import jakarta.persistence.metamodel.SingularAttribute;

/**
 * A rule for sorting an entity type in a query result set.
 * <p>
 * This is a convenience class which allows query result ordering
 * rules to be passed around the system before being applied to
 * a {@link Query} by calling {@link Query#sort(Sort[])}.
 *
 * @param <X> The type of the entity to be sorted
 *
 * @see Query#sort(Sort[])
 *
 * @author Gavin King
 *
 * @since 6.3
 */
public class Sort<X> {
    private final SortOrder order;
    private final SingularAttribute<X,?> attribute;
    private final Class<X> entityClass;
    private final String attributeName;

    public Sort(SortOrder order, SingularAttribute<X, ?> attribute) {
        this.order = order;
        this.attribute = attribute;
        this.attributeName = attribute.getName();
        this.entityClass = attribute.getDeclaringType().getJavaType();
    }

    public Sort(SortOrder order, Class<X> entityClass, String attributeName) {
        this.order = order;
        this.entityClass = entityClass;
        this.attributeName = attributeName;
        this.attribute = null;
    }

    public static <T> Sort<T> asc(SingularAttribute<T,?> attribute) {
        return new Sort<>(SortOrder.ASCENDING, attribute);
    }

    public static <T> Sort<T> desc(SingularAttribute<T,?> attribute) {
        return new Sort<>(SortOrder.ASCENDING, attribute);
    }

    public static <T> Sort<T> asc(Class<T> entityClass, String attributeName) {
        return new Sort<>( SortOrder.ASCENDING, entityClass, attributeName );
    }

    public static <T> Sort<T> desc(Class<T> entityClass, String attributeName) {
        return new Sort<>( SortOrder.ASCENDING, entityClass, attributeName );
    }

    public SortOrder getOrder() {
        return order;
    }

    public SingularAttribute<X, ?> getAttribute() {
        return attribute;
    }

    public Class<X> getEntityClass() {
        return entityClass;
    }

    public String getAttributeName() {
        return attributeName;
    }

    @Override
    public String toString() {
        return attributeName + " " + order;
    }

    @Override
    public boolean equals(Object o) {
        if ( o instanceof Sort ) {
            Sort<?> that = (Sort<?>) o;
            return that.order == order
                && that.attributeName.equals(attributeName)
                && that.entityClass.equals(entityClass);
        }
        else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return attributeName.hashCode() + entityClass.hashCode();
    }
}

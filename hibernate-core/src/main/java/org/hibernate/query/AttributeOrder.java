package org.hibernate.query;

import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.Nulls;
import jakarta.persistence.metamodel.SingularAttribute;

/**
 * An {@link Order} based on an attribute of an entity.
 *
 * @param <X> The entity type
 *
 * @author Gavin King
 */
record AttributeOrder<X>
		(@Nonnull SortDirection direction, @Nonnull Nulls nullPrecedence, @Nonnull SingularAttribute<X, ?> attribute, boolean caseSensitive)
		implements Order<X> {

	AttributeOrder(@Nonnull SortDirection order, @Nonnull Nulls nullPrecedence, @Nonnull SingularAttribute<X, ?> attribute) {
		this( order, nullPrecedence, attribute, true );
	}

	private AttributeOrder(@Nonnull AttributeOrder<X> that, @Nonnull Nulls nullPrecedence) {
		this( that.direction, nullPrecedence, that.attribute, that.caseSensitive );
	}

	private AttributeOrder(@Nonnull AttributeOrder<X> that, @Nonnull SortDirection direction) {
		this( direction, that.nullPrecedence, that.attribute, that.caseSensitive );
	}

	private AttributeOrder(@Nonnull AttributeOrder<X> that, boolean caseSensitive) {
		this( that.direction, that.nullPrecedence, that.attribute, caseSensitive );
	}

	@Override
	@Nonnull
	public Class<X> entityClass() {
		return attribute.getDeclaringType().getJavaType();
	}

	@Override
	public int element() {
		return 1;
	}

	@Override
	@Nonnull
	public String attributeName() {
		return attribute.getName();
	}

	@Override
	@Nonnull
	public Order<X> ignoringCase() {
		return new AttributeOrder<>( this, true );
	}

	@Override
	@Nonnull
	public Order<X> reverse() {
		return new AttributeOrder<>( this, direction.reverse() );
	}

	@Override
	@Nonnull
	public Order<X> withNullsFirst() {
		return new AttributeOrder<>( this, Nulls.FIRST );
	}

	@Override
	@Nonnull
	public Order<X> withNullsLast() {
		return new AttributeOrder<>( this, Nulls.LAST );
	}

	@Override
	@Nonnull
	public String toString() {
		return attribute.getName() + " " + direction;
	}

}

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import java.io.Serializable;
import java.util.Collection;

import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.metamodel.model.domain.AbstractManagedType;
import org.hibernate.metamodel.model.domain.CollectionDomainType;
import org.hibernate.metamodel.model.domain.PluralPersistentAttribute;
import org.hibernate.metamodel.model.domain.SimpleDomainType;
import org.hibernate.query.sqm.produce.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.domain.SqmPluralValuedSimplePath;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * @param <D> The (D)eclaring type
 * @param <C> The {@link Collection} type
 * @param <E> The type of the Collection's elements
 *
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
public abstract class AbstractPluralAttribute<D,C,E>
		extends AbstractAttribute<D,C,E>
		implements PluralPersistentAttribute<D,C,E>, CollectionDomainType<C,E>, Serializable {

	public static <X,C,E,K> PluralAttributeBuilder<X,C,E,K> create(
			AbstractManagedType<X> ownerType,
			SimpleDomainType<E> attrType,
			JavaTypeDescriptor<C> collectionClass,
			SimpleDomainType<K> keyType) {
		return new PluralAttributeBuilder<>( ownerType, attrType, collectionClass, keyType );
	}

	private final CollectionClassification classification;

	protected AbstractPluralAttribute(PluralAttributeBuilder<D,C,E,?> builder) {
		super(
				builder.getDeclaringType(),
				builder.getProperty().getName(),
				builder.getCollectionJavaTypeDescriptor(),
				builder.getAttributeClassification(),
				builder.getValueType(),
				builder.getMember()
		);

		this.classification = builder.getCollectionClassification();
	}

	@Override
	public CollectionClassification getCollectionClassification() {
		return classification;
	}

	@Override
	public CollectionType getCollectionType() {
		return getCollectionClassification().toJpaClassification();
	}

	@Override
	public CollectionDomainType<C, E> getType() {
		return this;
	}

	@Override
	public String getTypeName() {
		// todo (6.0) : this should return the "role"
		//		- for now just return the name of the collection type
		return getCollectionType().name();
	}

	@Override
	public SimpleDomainType<E> getElementType() {
		return getValueGraphType();
	}

	@Override
	@SuppressWarnings("unchecked")
	public SimpleDomainType<E> getValueGraphType() {
		return (SimpleDomainType<E>) super.getValueGraphType();
	}

	@Override
	public SimpleDomainType<?> getKeyGraphType() {
		return null;
	}

	@Override
	public boolean isAssociation() {
		return getPersistentAttributeType() == PersistentAttributeType.ONE_TO_MANY
				|| getPersistentAttributeType() == PersistentAttributeType.MANY_TO_MANY;
	}

	@Override
	public boolean isCollection() {
		return true;
	}

	@Override
	public BindableType getBindableType() {
		return BindableType.PLURAL_ATTRIBUTE;
	}

	@Override
	public Class<E> getBindableJavaType() {
		return getElementType().getJavaType();
	}

	@Override
	public Class<C> getJavaType() {
		return getJavaTypeDescriptor().getJavaType();
	}

	@Override
	public SqmPath<C> createSqmPath(SqmPath<?> lhs, SqmCreationState creationState) {
		return new SqmPluralValuedSimplePath(  );
	}
}

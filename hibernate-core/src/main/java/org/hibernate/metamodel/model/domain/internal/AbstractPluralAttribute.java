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
import org.hibernate.metamodel.model.domain.PluralPersistentAttribute;
import org.hibernate.metamodel.model.domain.SimpleDomainType;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.SqmPathSource;
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
		implements PluralPersistentAttribute<D,C,E>, Serializable {

	private final CollectionClassification classification;
	private final SqmPathSource<E> elementPathSource;

	@SuppressWarnings("WeakerAccess")
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

		this.elementPathSource = DomainModelHelper.resolveSqmPathSource(
				getName(),
				builder.getValueType(),
				BindableType.PLURAL_ATTRIBUTE
		);
	}

	@Override
	public String getPathName() {
		return getName();
	}

	@Override
	public CollectionClassification getCollectionClassification() {
		return classification;
	}

	@Override
	public SqmPathSource<E> getElementPathSource() {
		return elementPathSource;
	}

	@Override
	public SqmPathSource<?> findSubPathSource(String name) {
		return elementPathSource.findSubPathSource( name );
	}

	@Override
	public CollectionType getCollectionType() {
		return getCollectionClassification().toJpaClassification();
	}

	@Override
	public JavaTypeDescriptor<E> getExpressableJavaTypeDescriptor() {
		return getElementType().getExpressableJavaTypeDescriptor();
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
	public SimpleDomainType getKeyGraphType() {
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
	public SqmPath<E> createSqmPath(SqmPath<?> lhs, SqmCreationState creationState) {
		final NavigablePath navigablePath = lhs.getNavigablePath().append( getPathName() );
		//noinspection unchecked
		return new SqmPluralValuedSimplePath(
				navigablePath,
				this,
				lhs,
				creationState.getCreationContext().getNodeBuilder()
		);
	}
}

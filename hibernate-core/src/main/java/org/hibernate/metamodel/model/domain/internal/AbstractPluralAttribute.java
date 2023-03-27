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
import org.hibernate.metamodel.internal.MetadataContext;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.model.domain.PluralPersistentAttribute;
import org.hibernate.metamodel.model.domain.SimpleDomainType;
import org.hibernate.spi.NavigablePath;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.internal.SqmMappingModelHelper;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.domain.SqmPluralValuedSimplePath;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @param <D> The (D)eclaring type
 * @param <C> The {@link Collection} type
 * @param <E> The type of the Collection's elements
 *
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
public abstract class AbstractPluralAttribute<D, C, E>
		extends AbstractAttribute<D, C, E>
		implements PluralPersistentAttribute<D, C, E>, Serializable {

	private final CollectionClassification classification;
	private final SqmPathSource<E> elementPathSource;

	protected AbstractPluralAttribute(
			PluralAttributeBuilder<D,C,E,?> builder,
			MetadataContext metadataContext) {
		super(
				builder.getDeclaringType(),
				builder.getProperty().getName(),
				builder.getCollectionJavaType(),
				builder.getAttributeClassification(),
				builder.getValueType(),
				builder.getMember(),
				metadataContext
		);

		this.classification = builder.getCollectionClassification();

		this.elementPathSource = SqmMappingModelHelper.resolveSqmPathSource(
				CollectionPart.Nature.ELEMENT.getName(),
				builder.getValueType(),
				BindableType.PLURAL_ATTRIBUTE,
				builder.isGeneric()
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
		if ( CollectionPart.Nature.ELEMENT.getName().equals( name ) ) {
			return elementPathSource;
		}
		return elementPathSource.findSubPathSource( name );
	}

	@Override
	public SqmPathSource<?> getIntermediatePathSource(SqmPathSource<?> pathSource) {
		return pathSource.getPathName().equals( elementPathSource.getPathName() ) ? null : elementPathSource;
	}

	@Override
	public CollectionType getCollectionType() {
		return getCollectionClassification().toJpaClassification();
	}

	@Override
	public JavaType<E> getExpressibleJavaType() {
		return getElementType().getExpressibleJavaType();
	}

	@Override
	public SimpleDomainType<E> getElementType() {
		return getValueGraphType();
	}

	@Override
	public Class<C> getJavaType() {
		return getAttributeJavaType().getJavaTypeClass();
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
	public SqmPath<E> createSqmPath(SqmPath<?> lhs, SqmPathSource<?> intermediatePathSource) {
		final NavigablePath navigablePath;
		if ( intermediatePathSource == null ) {
			navigablePath = lhs.getNavigablePath().append( getPathName() );
		}
		else {
			navigablePath = lhs.getNavigablePath().append( intermediatePathSource.getPathName() ).append( getPathName() );
		}
		return new SqmPluralValuedSimplePath<>(
				navigablePath,
				this,
				lhs,
				lhs.nodeBuilder()
		);
	}

	@Override
	public boolean isGeneric() {
		return elementPathSource.isGeneric();
	}
}

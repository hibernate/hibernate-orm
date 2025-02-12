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
import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.PluralPersistentAttribute;
import org.hibernate.metamodel.model.domain.SimpleDomainType;
import org.hibernate.metamodel.model.domain.spi.JpaMetamodelImplementor;
import org.hibernate.spi.NavigablePath;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.internal.SqmMappingModelHelper;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.domain.SqmPluralValuedSimplePath;
import org.hibernate.type.descriptor.java.JavaType;

import static jakarta.persistence.metamodel.Bindable.BindableType.PLURAL_ATTRIBUTE;
import static org.hibernate.query.sqm.spi.SqmCreationHelper.buildSubNavigablePath;

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
				PLURAL_ATTRIBUTE,
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
	public SqmPathSource<?> findSubPathSource(String name, JpaMetamodelImplementor metamodel) {
		if ( CollectionPart.Nature.ELEMENT.getName().equals( name ) ) {
			return elementPathSource;
		}
		return elementPathSource.findSubPathSource( name, metamodel );
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
		return PLURAL_ATTRIBUTE;
	}

	@Override
	public Class<E> getBindableJavaType() {
		return getElementType().getJavaType();
	}

	@SuppressWarnings("unchecked")
	@Override
	public SqmPath<E> createSqmPath(SqmPath<?> lhs, SqmPathSource<?> intermediatePathSource) {
		// We need an unchecked cast here : PluralPersistentAttribute implements path source with its element type
		//  but resolving paths from it must produce collection-typed expressions.
		return (SqmPath<E>) new SqmPluralValuedSimplePath<>(
				PathHelper.append( lhs, this, intermediatePathSource ),
				this,
				lhs,
				lhs.nodeBuilder()
		);
	}

	@Override
	public NavigablePath createNavigablePath(SqmPath<?> parent, String alias) {
		if ( parent == null ) {
			throw new IllegalArgumentException(
					"`lhs` cannot be null for a sub-navigable reference - " + getName()
			);
		}
		final SqmPathSource<?> parentPathSource = parent.getResolvedModel();
		NavigablePath navigablePath = parent.getNavigablePath();
		if ( parentPathSource instanceof PluralPersistentAttribute<?, ?, ?> ) {
			navigablePath = navigablePath.append( CollectionPart.Nature.ELEMENT.getName() );
		}
		final DomainType<?> parentType = parentPathSource.getSqmPathType();
		if ( parentType != getDeclaringType() && parentType instanceof EntityDomainType &&
				( (EntityDomainType<?>) parentType ).findPluralAttribute( getName() ) == null ) {
			// If the parent path is an entity type which does not contain the joined attribute
			// add an implicit treat to the parent's navigable path
			navigablePath = navigablePath.treatAs( getDeclaringType().getTypeName() );
		}
		return buildSubNavigablePath( navigablePath, getName(), alias );
	}

	@Override
	public boolean isGeneric() {
		return elementPathSource.isGeneric();
	}
}

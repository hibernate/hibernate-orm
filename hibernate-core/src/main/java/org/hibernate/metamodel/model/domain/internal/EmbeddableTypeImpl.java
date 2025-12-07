/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.model.domain.internal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.AssertionFailure;
import org.hibernate.metamodel.UnsupportedMappingException;
import org.hibernate.metamodel.mapping.EntityDiscriminatorMapping;
import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.metamodel.model.domain.spi.JpaMetamodelImplementor;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.domain.SqmDomainType;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.domain.SqmEmbeddableDomainType;
import org.hibernate.type.descriptor.java.JavaType;

import static jakarta.persistence.metamodel.Bindable.BindableType.SINGULAR_ATTRIBUTE;
import static jakarta.persistence.metamodel.Type.PersistenceType.EMBEDDABLE;

/**
 * Implementation of {@link jakarta.persistence.metamodel.EmbeddableType}.
 *
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
public class EmbeddableTypeImpl<J>
		extends AbstractManagedType<J>
		implements SqmEmbeddableDomainType<J>, Serializable {
	private final boolean isDynamic;
	private final EmbeddedDiscriminatorSqmPathSource<?> discriminatorPathSource;
	private final List<SqmEmbeddableDomainType<? extends J>> subtypes = new ArrayList<>();

	public EmbeddableTypeImpl(
			JavaType<J> javaType,
			ManagedDomainType<? super J> superType,
			DomainType<?> discriminatorType,
			boolean isDynamic,
			JpaMetamodelImplementor domainMetamodel) {
		super( javaType.getTypeName(), javaType, superType, domainMetamodel );
		this.isDynamic = isDynamic;
		discriminatorPathSource =
				discriminatorType == null ? null
						: new EmbeddedDiscriminatorSqmPathSource<>( discriminatorType, this );
	}

	@Override
	public Class<J> getBindableJavaType() {
		return getJavaType();
	}

	@Override
	public PersistenceType getPersistenceType() {
		return EMBEDDABLE;
	}

	@Override
	public int getTupleLength() {
		int count = 0;
		for ( var attribute : getSingularAttributes() ) {
			if ( attribute.getType() instanceof SqmDomainType<?> domainType ) {
				count += domainType.getTupleLength();
			}
			else {
				throw new AssertionFailure( "Should have been a domain type" );
			}
		}
		return count;
	}

	@Override
	public Collection<? extends SqmEmbeddableDomainType<? extends J>> getSubTypes() {
		return subtypes;
	}

	@Override
	public void addSubType(ManagedDomainType<? extends J> subType) {
		super.addSubType( subType );
		if ( subType instanceof SqmEmbeddableDomainType<? extends J> entityDomainType ) {
			subtypes.add( entityDomainType );
		}
	}

	@Override
	public String getPathName() {
		return getTypeName();
	}

//	@Override
//	public SqmEmbeddableDomainType<J> getPathType() {
//		return this;
//	}

//	@Override
//	public SqmEmbeddableDomainType<J> getSqmType() {
//		return this;
//	}

	@Override
	public @Nullable SqmPathSource<?> findSubPathSource(String name) {
		final var attribute = getPathType().findAttribute( name );
		if ( attribute != null ) {
			return (SqmPathSource<?>) attribute;
		}

		final var subtypeAttribute = findSubTypesAttribute( name );
		if ( subtypeAttribute != null ) {
			return (SqmPathSource<?>) subtypeAttribute;
		}

		if ( EntityDiscriminatorMapping.matchesRoleName( name ) ) {
			return discriminatorPathSource;
		}

		return null;
	}

	@Override
	public SqmPath<J> createSqmPath(SqmPath<?> lhs, @Nullable SqmPathSource<?> intermediatePathSource) {
		throw new UnsupportedMappingException( "EmbeddableType cannot be used to create an SqmPath" );
	}

	@Override
	public BindableType getBindableType() {
		return SINGULAR_ATTRIBUTE;
	}
}

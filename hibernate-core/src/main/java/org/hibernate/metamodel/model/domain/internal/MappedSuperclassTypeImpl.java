/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.model.domain.internal;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.mapping.MappedSuperclass;
import org.hibernate.metamodel.UnsupportedMappingException;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.model.domain.IdentifiableDomainType;
import org.hibernate.metamodel.model.domain.spi.JpaMetamodelImplementor;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.domain.SqmDomainType;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.domain.SqmMappedSuperclassDomainType;
import org.hibernate.query.sqm.tree.domain.SqmPersistentAttribute;
import org.hibernate.type.descriptor.java.JavaType;

import static jakarta.persistence.metamodel.Bindable.BindableType.ENTITY_TYPE;
import static jakarta.persistence.metamodel.Type.PersistenceType.MAPPED_SUPERCLASS;

/**
 * Implementation of {@link jakarta.persistence.metamodel.MappedSuperclassType}.
 *
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
public class MappedSuperclassTypeImpl<J>
		extends AbstractIdentifiableType<J>
		implements SqmMappedSuperclassDomainType<J>, SqmPathSource<J> {

	public MappedSuperclassTypeImpl(
			String name,
			boolean hasIdClass,
			boolean hasIdProperty,
			boolean hasVersion,
			JavaType<J> javaType,
			IdentifiableDomainType<? super J> superType,
			JpaMetamodelImplementor jpaMetamodel) {
		super(
				name,
				javaType,
				superType,
				hasIdClass,
				hasIdProperty,
				hasVersion,
				jpaMetamodel
		);
	}

	public MappedSuperclassTypeImpl(
			JavaType<J> javaType,
			MappedSuperclass mappedSuperclass,
			IdentifiableDomainType<? super J> superType,
			JpaMetamodelImplementor jpaMetamodel) {
		this(
				javaType.getTypeName(),
				mappedSuperclass.getDeclaredIdentifierMapper() != null
						|| superType != null && superType.hasIdClass(),
				mappedSuperclass.hasIdentifierProperty(),
				mappedSuperclass.isVersioned(),
				javaType,
				superType,
				jpaMetamodel
		);
	}

	@Override
	public Class<J> getBindableJavaType() {
		return getJavaType();
	}

	@Override
	public @Nullable SqmDomainType<J> getSqmType() {
		return this;
	}

	@Override
	public String getPathName() {
		return getTypeName();
	}

	@Override
	public SqmMappedSuperclassDomainType<J> getPathType() {
		return this;
	}

	@Override
	public @Nullable SqmPathSource<?> findSubPathSource(String name) {
		final var attribute = findAttribute( name );
		if ( attribute != null ) {
			return (SqmPathSource<?>) attribute;
		}
		else if ( "id".equalsIgnoreCase( name ) ) {
			return hasIdClass() ? getIdentifierDescriptor() : null;
		}
		else {
			return null;
		}
	}

	@Override
	public @Nullable SqmPathSource<?> getIdentifierDescriptor() {
		return super.getIdentifierDescriptor();
	}

	@Override
	public @Nullable SqmPersistentAttribute<? super J, ?> findAttribute(String name) {
		final var attribute = super.findAttribute( name );
		if ( attribute != null ) {
			return attribute;
		}
		else if ( EntityIdentifierMapping.matchesRoleName( name ) ) {
			return findIdAttribute();
		}
		else {
			return null;
		}
	}

	@Override
	public BindableType getBindableType() {
		return ENTITY_TYPE;
	}

	@Override
	public PersistenceType getPersistenceType() {
		return MAPPED_SUPERCLASS;
	}

	@Override
	protected boolean isIdMappingRequired() {
		return false;
	}

	@Override
	public SqmPath<J> createSqmPath(SqmPath<?> lhs, @Nullable SqmPathSource<?> intermediatePathSource) {
		throw new UnsupportedMappingException(
				"MappedSuperclassType cannot be used to create an SqmPath - that would be an SqmFrom which are created directly"
		);
	}
}

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import org.hibernate.mapping.MappedSuperclass;
import org.hibernate.metamodel.UnsupportedMappingException;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.model.domain.AbstractIdentifiableType;
import org.hibernate.metamodel.model.domain.IdentifiableDomainType;
import org.hibernate.metamodel.model.domain.MappedSuperclassDomainType;
import org.hibernate.metamodel.model.domain.PersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.JpaMetamodelImplementor;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * Implementation of {@link jakarta.persistence.metamodel.MappedSuperclassType}.
 *
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
public class MappedSuperclassTypeImpl<J> extends AbstractIdentifiableType<J> implements MappedSuperclassDomainType<J> {

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
	public String getPathName() {
		return getTypeName();
	}

	@Override
	public MappedSuperclassDomainType<J> getSqmPathType() {
		return this;
	}

	@Override
	public SqmPathSource<?> findSubPathSource(String name) {
		final PersistentAttribute<?,?> attribute = findAttribute( name );
		if ( attribute != null ) {
			return (SqmPathSource<?>) attribute;
		}

		if ( "id".equalsIgnoreCase( name ) ) {
			if ( hasIdClass() ) {
				return getIdentifierDescriptor();
			}
		}

		return null;
	}

	@Override
	public PersistentAttribute<? super J, ?> findAttribute(String name) {
		final PersistentAttribute<? super J, ?> attribute = super.findAttribute( name );
		if ( attribute != null ) {
			return attribute;
		}

		if ( EntityIdentifierMapping.matchesRoleName( name ) ) {
			return findIdAttribute();
		}

		return null;
	}

	@Override
	public BindableType getBindableType() {
		return BindableType.ENTITY_TYPE;
	}

	@Override
	public PersistenceType getPersistenceType() {
		return PersistenceType.MAPPED_SUPERCLASS;
	}

	@Override
	protected boolean isIdMappingRequired() {
		return false;
	}

	@Override
	public SqmPath<J> createSqmPath(SqmPath<?> lhs, SqmPathSource<?> intermediatePathSource) {
		throw new UnsupportedMappingException(
				"MappedSuperclassType cannot be used to create an SqmPath - that would be an SqmFrom which are created directly"
		);
	}
}

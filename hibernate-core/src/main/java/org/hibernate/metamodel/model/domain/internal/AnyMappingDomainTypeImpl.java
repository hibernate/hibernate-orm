/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.model.domain.internal;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.mapping.Any;
import org.hibernate.mapping.Column;
import org.hibernate.metamodel.model.domain.AnyMappingDomainType;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.model.domain.SimpleDomainType;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.query.sqm.tree.domain.SqmDomainType;
import org.hibernate.type.AnyType;
import org.hibernate.type.BasicType;
import org.hibernate.type.MetaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.internal.ConvertedBasicTypeImpl;

import java.util.List;

import static jakarta.persistence.metamodel.Type.PersistenceType.ENTITY;
import static org.hibernate.metamodel.mapping.internal.AnyDiscriminatorPart.determineDiscriminatorConverter;

/**
 * @author Steve Ebersole
 */
public class AnyMappingDomainTypeImpl<T> implements AnyMappingDomainType<T>, SqmDomainType<T> {
	private final AnyType anyType;
	private final JavaType<T> baseJtd;
	private final BasicType<Class<?>> anyDiscriminatorType;

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public AnyMappingDomainTypeImpl(
			Any bootAnyMapping,
			AnyType anyType,
			JavaType<T> baseJtd,
			MappingMetamodelImplementor mappingMetamodel) {
		this.anyType = anyType;
		this.baseJtd = baseJtd;

		final MetaType discriminatorType = (MetaType) anyType.getDiscriminatorType();
		final BasicType discriminatorBaseType = (BasicType) discriminatorType.getBaseType();
		final NavigableRole navigableRole = resolveNavigableRole( bootAnyMapping );

		anyDiscriminatorType = new ConvertedBasicTypeImpl(
				navigableRole.getFullPath(),
				discriminatorBaseType.getJdbcType(),
				determineDiscriminatorConverter(
						navigableRole,
						discriminatorBaseType,
						bootAnyMapping.getMetaValues(),
						discriminatorType.getImplicitValueStrategy(),
						mappingMetamodel
				)
		);
	}

	@Override
	public @Nullable SqmDomainType<T> getSqmType() {
		return this;
	}

//	@Override
//	public Class<T> getJavaType() {
//		return AnyMappingDomainType.super.getJavaType();
//	}

	@Override
	public String getTypeName() {
		return baseJtd.getTypeName();
	}

	private NavigableRole resolveNavigableRole(Any bootAnyMapping) {
		final StringBuilder buffer = new StringBuilder();
		if ( bootAnyMapping.getTable() != null ) {
			buffer.append( bootAnyMapping.getTable().getName() );
		}

		buffer.append( "(" );
		final List<Column> columns = bootAnyMapping.getColumns();
		for ( int i = 0; i < columns.size(); i++ ) {
			buffer.append( columns.get( i ).getName() );
			if ( i+1 < columns.size() ) {
				// still more columns
				buffer.append( "," );
			}
		}
		buffer.append( ")" );

		return new NavigableRole( buffer.toString() );
	}

	@Override
	public PersistenceType getPersistenceType() {
		return ENTITY;
	}

	@Override
	public Class<T> getJavaType() {
		return baseJtd.getJavaTypeClass();
	}

	@Override
	public JavaType<T> getExpressibleJavaType() {
		return baseJtd;
	}

	@Override
	public BasicType<Class<?>> getDiscriminatorType() {
		return anyDiscriminatorType;
	}

	@Override
	public SimpleDomainType<?> getKeyType() {
		return (BasicType<?>) anyType.getIdentifierType();
	}

}

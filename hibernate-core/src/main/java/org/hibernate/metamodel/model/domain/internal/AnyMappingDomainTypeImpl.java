/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.metamodel.model.domain.internal;

import org.hibernate.mapping.Any;
import org.hibernate.mapping.Column;
import org.hibernate.metamodel.mapping.DefaultDiscriminatorConverter;
import org.hibernate.metamodel.mapping.MappedDiscriminatorConverter;
import org.hibernate.metamodel.model.domain.AnyMappingDomainType;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.model.domain.SimpleDomainType;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.type.AnyType;
import org.hibernate.type.BasicType;
import org.hibernate.type.MetaType;
import org.hibernate.type.descriptor.java.ClassJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.internal.ConvertedBasicTypeImpl;

import java.util.List;

/**
 * @author Steve Ebersole
 */
public class AnyMappingDomainTypeImpl<T> implements AnyMappingDomainType<T> {
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

		anyDiscriminatorType = new ConvertedBasicTypeImpl<>(
				navigableRole.getFullPath(),
				discriminatorBaseType.getJdbcType(),
				bootAnyMapping.getMetaValues().isEmpty()
				? DefaultDiscriminatorConverter.fromMappingMetamodel(
						navigableRole,
						ClassJavaType.INSTANCE,
						discriminatorBaseType,
						mappingMetamodel
				)
				: MappedDiscriminatorConverter.fromValueMappings(
						navigableRole,
						ClassJavaType.INSTANCE,
						discriminatorBaseType,
						bootAnyMapping.getMetaValues(),
						mappingMetamodel
				)
		);
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
		return PersistenceType.ENTITY;
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

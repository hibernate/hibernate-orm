/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.Any;
import org.hibernate.mapping.Column;
import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.metamodel.mapping.DiscriminatorConverter;
import org.hibernate.metamodel.mapping.internal.AnyDiscriminatorPart;
import org.hibernate.metamodel.model.domain.AnyMappingDomainType;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.model.domain.SimpleDomainType;
import org.hibernate.type.AnyType;
import org.hibernate.type.BasicType;
import org.hibernate.type.MetaType;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.descriptor.java.ClassJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.ObjectJavaType;
import org.hibernate.type.internal.ConvertedBasicTypeImpl;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public class AnyMappingDomainTypeImpl implements AnyMappingDomainType<Class> {
	private final AnyType anyType;
	private final JavaType<Class> baseJtd;
	private final BasicType<Class> anyDiscriminatorType;

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public AnyMappingDomainTypeImpl(
			Any bootAnyMapping,
			AnyType anyType,
			JavaType<Class> baseJtd,
			TypeConfiguration typeConfiguration,
			MappingMetamodel mappingMetamodel,
			SessionFactoryImplementor sessionFactory) {
		this.anyType = anyType;
		this.baseJtd = baseJtd;

		final MetaType discriminatorType = (MetaType) anyType.getDiscriminatorType();
		final BasicType discriminatorBaseType = (BasicType) discriminatorType.getBaseType();
		final NavigableRole navigableRole = resolveNavigableRole( bootAnyMapping );

		anyDiscriminatorType = new ConvertedBasicTypeImpl<>(
				navigableRole.getFullPath(),
				discriminatorBaseType.getJdbcType(),
				DiscriminatorConverter.fromValueMappings(
						navigableRole,
						(JavaType) ClassJavaType.INSTANCE,
						discriminatorBaseType,
						bootAnyMapping.getMetaValues(),
						sessionFactory
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
	@SuppressWarnings("unchecked")
	public Class<Class> getJavaType() {
		return (Class<Class>) anyType.getReturnedClass();
	}

	@Override
	public JavaType<Class> getExpressibleJavaType() {
		return baseJtd;
	}

	@Override
	public BasicType<Class> getDiscriminatorType() {
		return anyDiscriminatorType;
	}

	@Override
	public SimpleDomainType getKeyType() {
		return (BasicType<?>) anyType.getIdentifierType();
	}

}

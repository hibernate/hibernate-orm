/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.binder.internal;

import java.util.Collections;

import org.hibernate.AssertionFailure;
import org.hibernate.MappingException;
import org.hibernate.annotations.TenantId;
import org.hibernate.binder.AttributeBinder;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Formula;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Selectable;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.JavaType;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;

/**
 * Sets up filters associated with a {@link TenantId} field
 *
 * @author Gavin King
 */
public class TenantIdBinder implements AttributeBinder<TenantId> {

	public static final String FILTER_NAME = "_tenantId";
	public static final String PARAMETER_NAME = "tenantId";

	@Override
	public void bind(
			TenantId tenantId,
			MetadataBuildingContext buildingContext,
			PersistentClass persistentClass,
			Property property) {
		final InFlightMetadataCollector collector = buildingContext.getMetadataCollector();

		final String returnedClassName = property.getReturnedClassName();
		final BasicType<Object> tenantIdType =
				collector.getTypeConfiguration().getBasicTypeRegistry()
						.getRegisteredType( returnedClassName );

		final FilterDefinition filterDefinition = collector.getFilterDefinition( FILTER_NAME );
		if ( filterDefinition == null ) {
			collector.addFilterDefinition(
					new FilterDefinition(
							FILTER_NAME,
							"",
							false,
							true,
							singletonMap( PARAMETER_NAME, tenantIdType ),
							Collections.emptyMap()
					)
			);
		}
		else {
			final JavaType<?> tenantIdTypeJtd = tenantIdType.getJavaTypeDescriptor();
			final JdbcMapping jdbcMapping = filterDefinition.getParameterJdbcMapping( PARAMETER_NAME );
			assert jdbcMapping != null;
			final JavaType<?> parameterJavaType = jdbcMapping.getJavaTypeDescriptor();
			if ( !parameterJavaType.getJavaTypeClass().equals( tenantIdTypeJtd.getJavaTypeClass() ) ) {
				throw new MappingException(
						"all @TenantId fields must have the same type: "
								+ parameterJavaType.getTypeName()
								+ " differs from "
								+ tenantIdTypeJtd.getTypeName()
				);
			}
		}
		persistentClass.addFilter(
				FILTER_NAME,
				columnNameOrFormula( property )
						+ " = :"
						+ PARAMETER_NAME,
				true,
				emptyMap(),
				emptyMap()
		);

		property.resetUpdateable( false );
		property.resetOptional( false );
	}

	private String columnNameOrFormula(Property property) {
		if ( property.getColumnSpan() != 1 ) {
			throw new MappingException( "@TenantId attribute must be mapped to a single column or formula" );
		}
		final Selectable selectable = property.getSelectables().get( 0 );
		if ( selectable instanceof Formula formula ) {
			return formula.getFormula();
		}
		else if ( selectable instanceof Column column ) {
			return column.getName();
		}
		else {
			throw new AssertionFailure( "@TenantId attribute must be mapped to a column or formula" );
		}
	}

}

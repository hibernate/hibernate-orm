/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tuple;

import org.hibernate.MappingException;
import org.hibernate.annotations.TenantId;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Formula;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Selectable;
import org.hibernate.type.BasicType;
import org.hibernate.type.Type;

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
		InFlightMetadataCollector collector = buildingContext.getMetadataCollector();
		BasicType<Object> tenantIdType =
				collector.getTypeConfiguration().getBasicTypeRegistry()
						.getRegisteredType( property.getReturnedClassName() );
		FilterDefinition filterDefinition = collector.getFilterDefinition(FILTER_NAME);
		if ( filterDefinition == null ) {
			collector.addFilterDefinition(
					new FilterDefinition(
							FILTER_NAME,
							"",
							singletonMap( PARAMETER_NAME, tenantIdType )
					)
			);
		}
		else {
			Type parameterType = filterDefinition.getParameterTypes().get(PARAMETER_NAME);
			if ( !parameterType.getName().equals( tenantIdType.getName() ) ) {
				throw new MappingException(
						"all @TenantId fields must have the same type: "
								+ parameterType.getName()
								+ " differs from "
								+ tenantIdType.getName()
				);
			}
		}
		persistentClass.addFilter(
						FILTER_NAME,
						columnNameOrFormula(property)
								+ " = :"
								+ PARAMETER_NAME,
						true,
						emptyMap(),
						emptyMap()
				);

		property.resetUpdateable(false);
		property.resetOptional(false);
	}

	private String columnNameOrFormula(Property property) {
		if ( property.getColumnSpan()!=1 ) {
			throw new MappingException("@TenantId attribute must be mapped to a single column or formula");
		}
		Selectable column = property.getColumnIterator().next();
		return column.isFormula()
				? ((Formula) column).getFormula()
				: ((Column) column).getName();
	}

}

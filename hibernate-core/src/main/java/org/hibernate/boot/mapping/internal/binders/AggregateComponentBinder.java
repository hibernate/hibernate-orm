/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.binders;

import org.hibernate.annotations.Struct;
import org.hibernate.boot.model.relational.QualifiedName;
import org.hibernate.boot.mapping.internal.model.AggregateMappingIntent;
import org.hibernate.boot.mapping.internal.sources.ComponentSource;
import org.hibernate.boot.mapping.internal.context.BindingState;
import org.hibernate.mapping.AggregateColumn;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.java.spi.EmbeddableAggregateJavaType;

/**
 * New-pipeline support for aggregate embeddable mappings such as {@link Struct}.
 */
final class AggregateComponentBinder {
	private AggregateComponentBinder() {
	}

	static void processAggregate(
			PersistentClass ownerBinding,
			ComponentSource source,
			Component component,
			Table table,
			BindingState state) {
		final AggregateMappingIntent intent = source.aggregateMappingIntent();
		if ( !intent.isAggregate() ) {
			return;
		}

		final QualifiedName structName = intent.structName( state );
		final String structNameText = structName == null ? null : structName.render();
		final Integer aggregateJdbcTypeCode = intent.jdbcTypeCode();
		state.addJavaTypeRegistration(
				component.getComponentClass(),
				new EmbeddableAggregateJavaType<>( component.getComponentClass(), structNameText )
		);
		component.setStructName( structName );
		component.setStructColumnNames( intent.structAttributeNames() );

		final Column column = ColumnBinder.bindColumn(
				intent.aggregateColumnSource(),
				() -> source.sourceMember().resolveAttributeName()
		);
		final BasicValue aggregateValue = new BasicValue( state.getMetadataBuildingContext(), table );
		aggregateValue.setTable( table );
		aggregateValue.setTypeUsingReflection(
				source.sourceMember().getDeclaringType().getName(),
				source.sourceMember().resolveAttributeName()
		);
		if ( !intent.plural() ) {
			aggregateValue.setExplicitJavaTypeAccess(
					(typeConfiguration) -> new EmbeddableAggregateJavaType<>( component.getComponentClass(), structNameText )
			);
		}
		if ( aggregateJdbcTypeCode != null ) {
			aggregateValue.setExplicitJdbcTypeCode( aggregateJdbcTypeCode );
		}
		final AggregateColumn aggregateColumn = new AggregateColumn( column, component );
		aggregateColumn.setValue( aggregateValue );
		if ( structNameText != null && aggregateColumn.getSqlType() == null ) {
			if ( intent.plural() ) {
				aggregateColumn.setSqlTypeCode( getStructPluralSqlTypeCode( state ) );
				aggregateColumn.setSqlType(
						state.getDatabase()
								.getDialect()
								.getArrayTypeName( null, structNameText, null )
				);
			}
			else {
				aggregateColumn.setSqlTypeCode( SqlTypes.STRUCT );
				aggregateColumn.setSqlType( structNameText );
			}
		}
		else if ( aggregateJdbcTypeCode != null ) {
			aggregateColumn.setSqlTypeCode( aggregateJdbcTypeCode );
		}
		aggregateValue.addColumn( aggregateColumn );
		table.addColumn( aggregateColumn );
		component.setAggregateColumn( aggregateColumn );

		state.getMetadataBuildingContext().getMetadataCollector().addSecondPass(
				new org.hibernate.boot.model.internal.AggregateComponentSecondPass(
						table,
						ownerBinding.getEntityName(),
						component,
						source.componentType(),
						source.sourceMember().resolveAttributeName(),
						state.getMetadataBuildingContext()
				)
		);
	}

	private static int getStructPluralSqlTypeCode(BindingState state) {
		return switch ( state.getMetadataBuildingContext().getPreferredSqlTypeCodeForArray() ) {
			case SqlTypes.ARRAY -> SqlTypes.STRUCT_ARRAY;
			case SqlTypes.TABLE -> SqlTypes.STRUCT_TABLE;
			default -> throw new UnsupportedOperationException(
					"Dialect does not support structured array types: "
					+ state.getDatabase().getDialect().getClass().getName()
			);
		};
	}

}

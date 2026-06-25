/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.binders;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.MapKeyJdbcTypeCode;
import org.hibernate.annotations.Struct;
import org.hibernate.boot.model.relational.QualifiedName;
import org.hibernate.boot.model.relational.QualifiedNameImpl;
import org.hibernate.boot.mapping.internal.sources.ColumnSource;
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
		if ( !supportsAggregateBridge( source ) || !isAggregate( source ) ) {
			return;
		}

		final QualifiedName structName = determineStructName( source, state );
		final String structNameText = structName == null ? null : structName.render();
		final Integer aggregateJdbcTypeCode = determineAggregateJdbcTypeCode( source );
		state.addJavaTypeRegistration(
				component.getComponentClass(),
				new EmbeddableAggregateJavaType<>( component.getComponentClass(), structNameText )
		);
		component.setStructName( structName );
		component.setStructColumnNames( determineStructAttributeNames( source ) );

		final Column column = ColumnBinder.bindColumn(
				aggregateColumnSource( source ),
				() -> source.sourceMember().resolveAttributeName()
		);
		final BasicValue aggregateValue = new BasicValue( state.getMetadataBuildingContext(), table );
		aggregateValue.setTable( table );
		aggregateValue.setTypeUsingReflection(
				source.sourceMember().getDeclaringType().getName(),
				source.sourceMember().resolveAttributeName()
		);
		aggregateValue.setExplicitJavaTypeAccess(
				(typeConfiguration) -> new EmbeddableAggregateJavaType<>( component.getComponentClass(), structNameText )
		);
		if ( aggregateJdbcTypeCode != null ) {
			aggregateValue.setExplicitJdbcTypeCode( aggregateJdbcTypeCode );
		}
		final AggregateColumn aggregateColumn = new AggregateColumn( column, component );
		aggregateColumn.setValue( aggregateValue );
		if ( structNameText != null && aggregateColumn.getSqlType() == null ) {
			if ( isPluralAggregate( source ) ) {
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

	private static boolean supportsAggregateBridge(ComponentSource source) {
		return source.kind() == ComponentSource.Kind.EMBEDDED_ATTRIBUTE
			|| source.kind() == ComponentSource.Kind.COLLECTION_ELEMENT
			|| source.kind() == ComponentSource.Kind.MAP_KEY;
	}

	private static boolean isPluralAggregate(ComponentSource source) {
		return source.kind() == ComponentSource.Kind.COLLECTION_ELEMENT
				|| source.sourceMember().isArray();
	}

	private static boolean isAggregate(ComponentSource source) {
		return source.sourceMember() != null
				&& ( source.sourceMember().hasDirectAnnotationUsage( Struct.class )
					|| determineAggregateJdbcTypeCode( source ) != null
					|| source.componentType().hasDirectAnnotationUsage( Struct.class ) );
	}

	private static Integer determineAggregateJdbcTypeCode(ComponentSource source) {
		final Integer jdbcTypeCode = explicitAggregateJdbcTypeCode( source );
		if ( jdbcTypeCode == null ) {
			return null;
		}
		return switch ( jdbcTypeCode ) {
			case SqlTypes.STRUCT,
					SqlTypes.JSON,
					SqlTypes.SQLXML,
					SqlTypes.STRUCT_ARRAY,
					SqlTypes.STRUCT_TABLE,
					SqlTypes.JSON_ARRAY,
					SqlTypes.XML_ARRAY -> jdbcTypeCode;
			default -> null;
		};
	}

	private static Integer explicitAggregateJdbcTypeCode(ComponentSource source) {
		if ( source.kind() == ComponentSource.Kind.MAP_KEY ) {
			final MapKeyJdbcTypeCode jdbcTypeCode = source.sourceMember()
					.getDirectAnnotationUsage( MapKeyJdbcTypeCode.class );
			return jdbcTypeCode == null ? null : jdbcTypeCode.value();
		}
		final JdbcTypeCode jdbcTypeCode = source.sourceMember().getDirectAnnotationUsage( JdbcTypeCode.class );
		return jdbcTypeCode == null ? null : jdbcTypeCode.value();
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

	private static ColumnSource aggregateColumnSource(ComponentSource source) {
		if ( source.kind() == ComponentSource.Kind.MAP_KEY ) {
			return ColumnSource.from( source.sourceMember().getDirectAnnotationUsage( jakarta.persistence.MapKeyColumn.class ) );
		}
		return ColumnSource.from( source.sourceMember().getDirectAnnotationUsage( jakarta.persistence.Column.class ) );
	}

	private static QualifiedName determineStructName(ComponentSource source, BindingState state) {
		final Struct memberStruct = source.sourceMember().getDirectAnnotationUsage( Struct.class );
		if ( memberStruct != null ) {
			return toQualifiedName( memberStruct, state );
		}

		final Struct typeStruct = source.componentType().getDirectAnnotationUsage( Struct.class );
		return typeStruct == null ? null : toQualifiedName( typeStruct, state );
	}

	private static QualifiedName toQualifiedName(Struct struct, BindingState state) {
		final var database = state.getDatabase();
		return new QualifiedNameImpl(
				database.toIdentifier( struct.catalog() ),
				database.toIdentifier( struct.schema() ),
				database.toIdentifier( struct.name() )
		);
	}

	private static String[] determineStructAttributeNames(ComponentSource source) {
		final Struct memberStruct = source.sourceMember().getDirectAnnotationUsage( Struct.class );
		if ( memberStruct != null ) {
			return memberStruct.attributes();
		}

		final Struct typeStruct = source.componentType().getDirectAnnotationUsage( Struct.class );
		return typeStruct == null ? null : typeStruct.attributes();
	}
}

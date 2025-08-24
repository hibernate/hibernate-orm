/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;


import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Struct;
import org.hibernate.boot.model.relational.QualifiedName;
import org.hibernate.boot.model.relational.QualifiedNameImpl;
import org.hibernate.boot.model.relational.QualifiedNameParser;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.PropertyData;
import org.hibernate.mapping.AggregateColumn;
import org.hibernate.mapping.Component;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.java.spi.EmbeddableAggregateJavaType;

/**
 * Processes aggregate component annotations from Java classes and produces the Hibernate configuration-time metamodel,
 * that is, the objects defined in the package {@link org.hibernate.mapping}.
 */
public final class AggregateComponentBinder {

	private AggregateComponentBinder() {}

	public static void processAggregate(
			Component component,
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			ClassDetails componentClassDetails,
			AnnotatedColumns columns,
			MetadataBuildingContext context) {
		if ( isAggregate( inferredData.getAttributeMember(), componentClassDetails ) ) {
			final var metadataCollector = context.getMetadataCollector();
			final var typeConfiguration = metadataCollector.getTypeConfiguration();
			// Determine a struct name if this is a struct through some means
			final QualifiedName structQualifiedName =
					determineStructName( columns, inferredData, componentClassDetails, context );
			final String structName = structQualifiedName == null ? null : structQualifiedName.render();

			// We must register a special JavaType for the embeddable which can provide a recommended JdbcType
			typeConfiguration.getJavaTypeRegistry().resolveDescriptor(
					component.getComponentClass(),
					() -> new EmbeddableAggregateJavaType<>( component.getComponentClass(), structName )
			);
			component.setStructName( structQualifiedName );
			component.setStructColumnNames( determineStructAttributeNames( inferredData, componentClassDetails ) );

			// Determine the aggregate column
			final var basicValueBinder = new BasicValueBinder( BasicValueBinder.Kind.ATTRIBUTE, component, context );
			basicValueBinder.setReturnedClassName( inferredData.getClassOrElementType().getName() );
			basicValueBinder.setColumns( columns );
			basicValueBinder.setPersistentClassName( propertyHolder.getClassName() );
			basicValueBinder.setType(
					inferredData.getAttributeMember(),
					inferredData.getPropertyType(),
					inferredData.getDeclaringClass().getName(),
					null
			);
			final var propertyValue = basicValueBinder.make();
			final var aggregateColumn = (AggregateColumn) propertyValue.getColumn();
			if ( structName != null && aggregateColumn.getSqlType() == null ) {
				if ( inferredData.getAttributeMember().isArray() || inferredData.getAttributeMember().isPlural() ) {
					aggregateColumn.setSqlTypeCode( getStructPluralSqlTypeCode( context ) );
					aggregateColumn.setSqlType(
							context.getMetadataCollector()
									.getDatabase()
									.getDialect()
									.getArrayTypeName(
											null,
											structName,
											null
									)
					);
				}
				else {
					aggregateColumn.setSqlTypeCode( SqlTypes.STRUCT );
					aggregateColumn.setSqlType( structName );
				}
			}
			component.setAggregateColumn( aggregateColumn );

			context.getMetadataCollector().addSecondPass(
					new AggregateComponentSecondPass(
							propertyHolder,
							component,
							componentClassDetails,
							inferredData.getPropertyName(),
							context
					)
			);
		}
	}

	private static int getStructPluralSqlTypeCode(MetadataBuildingContext context) {
		return switch ( context.getPreferredSqlTypeCodeForArray() ) {
			case SqlTypes.ARRAY -> SqlTypes.STRUCT_ARRAY;
			case SqlTypes.TABLE -> SqlTypes.STRUCT_TABLE;
			default -> throw new UnsupportedOperationException(
					"Dialect does not support structured array types: "
					+ context.getMetadataCollector().getDatabase()
							.getDialect().getClass().getName()
			);
		};
	}

	private static QualifiedName determineStructName(
			AnnotatedColumns columns,
			PropertyData inferredData,
			ClassDetails returnedClassOrElement,
			MetadataBuildingContext context) {
		final var memberDetails = inferredData.getAttributeMember();
		if ( memberDetails != null ) {
			final var struct = memberDetails.getDirectAnnotationUsage( Struct.class );
			if ( struct != null ) {
				return toQualifiedName( struct, context );
			}
			else {
				final var jdbcTypeCode = memberDetails.getDirectAnnotationUsage( JdbcTypeCode.class );
				if ( jdbcTypeCode != null
						&& ( jdbcTypeCode.value() == SqlTypes.STRUCT
							|| jdbcTypeCode.value() == SqlTypes.STRUCT_ARRAY
							|| jdbcTypeCode.value() == SqlTypes.STRUCT_TABLE )
						&& columns != null ) {
					final var columnList = columns.getColumns();
					if ( columnList.size() == 1 ) {
						final String sqlType = columnList.get( 0 ).getSqlType();
						if ( sqlType != null ) {
							if ( sqlType.contains( "." ) ) {
								return QualifiedNameParser.INSTANCE.parse( sqlType );
							}
							else {
								return new QualifiedNameParser.NameParts(
										null,
										null,
										context.getMetadataCollector().getDatabase().toIdentifier( sqlType )
								);
							}
						}
					}
				}
			}
		}

		final var struct = returnedClassOrElement.getDirectAnnotationUsage( Struct.class );
		return struct == null ? null : toQualifiedName( struct, context );
	}

	private static QualifiedName toQualifiedName(Struct struct, MetadataBuildingContext context) {
		final var database = context.getMetadataCollector().getDatabase();
		return new QualifiedNameImpl(
				database.toIdentifier( struct.catalog() ),
				database.toIdentifier( struct.schema() ),
				database.toIdentifier( struct.name() )
		);
	}

	private static String[] determineStructAttributeNames(PropertyData inferredData, ClassDetails returnedClassOrElement) {
		final var memberDetails = inferredData.getAttributeMember();
		if ( memberDetails != null ) {
			final var struct = memberDetails.getDirectAnnotationUsage( Struct.class );
			if ( struct != null ) {
				return struct.attributes();
			}
		}
		final var struct = returnedClassOrElement.getDirectAnnotationUsage( Struct.class );
		return struct == null ? null : struct.attributes();
	}

	private static boolean isAggregate(MemberDetails property, ClassDetails returnedClass) {
		if ( property != null ) {
			if ( property.hasDirectAnnotationUsage( Struct.class ) ) {
				return true;
			}
			else {
				final var jdbcTypeCode = property.getDirectAnnotationUsage( JdbcTypeCode.class );
				if ( jdbcTypeCode != null ) {
					switch ( jdbcTypeCode.value() ) {
						case SqlTypes.STRUCT:
						case SqlTypes.JSON:
						case SqlTypes.SQLXML:
						case SqlTypes.STRUCT_ARRAY:
						case SqlTypes.STRUCT_TABLE:
						case SqlTypes.JSON_ARRAY:
						case SqlTypes.XML_ARRAY:
							return true;
					}
				}
			}
		}

		return returnedClass != null
			&& returnedClass.hasDirectAnnotationUsage( Struct.class );

	}
}

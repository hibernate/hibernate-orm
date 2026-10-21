/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;


import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Struct;
import org.hibernate.boot.model.relational.QualifiedName;
import org.hibernate.boot.model.relational.QualifiedNameImpl;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.PropertyData;
import org.hibernate.mapping.AggregateColumn;
import org.hibernate.mapping.Component;
import org.hibernate.models.spi.AnnotationTarget;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.java.spi.EmbeddableAggregateJavaType;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.boot.model.internal.BasicValueBinder.Kind.ATTRIBUTE;
import static org.hibernate.boot.model.internal.DefaultSchemaHelper.defaultSchema;

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
					determineStructName( inferredData, componentClassDetails, context );
			final String structName = structQualifiedName == null ? null : structQualifiedName.render();

			// We must register a special JavaType for the embeddable which can provide a recommended JdbcType
			registerDescriptor( component.getComponentClass(), typeConfiguration, structName );
			component.setStructName( structQualifiedName );
			component.setStructColumnNames( determineStructAttributeNames( inferredData, componentClassDetails ) );

			// Determine the aggregate column
			final var basicValueBinder = new BasicValueBinder( ATTRIBUTE, component, context );
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

	private static <T> void registerDescriptor(Class<T> componentClass, TypeConfiguration typeConfiguration, String structName) {
		typeConfiguration.getJavaTypeRegistry()
				.resolveDescriptor( componentClass,
						() -> new EmbeddableAggregateJavaType<>( componentClass, structName ) );
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
			PropertyData inferredData,
			ClassDetails returnedClassOrElement,
			MetadataBuildingContext context) {
		final var memberDetails = inferredData.getAttributeMember();
		if ( memberDetails != null ) {
			final var struct = memberDetails.getDirectAnnotationUsage( Struct.class );
			if ( struct != null ) {
				return toQualifiedName( struct, memberDetails, context );
			}
		}

		final var struct = returnedClassOrElement.getDirectAnnotationUsage( Struct.class );
		return struct == null ? null : toQualifiedName( struct, returnedClassOrElement, context );
	}

	private static QualifiedName toQualifiedName(
			Struct struct,
			AnnotationTarget annotationTarget,
			MetadataBuildingContext context) {
		final var database = context.getMetadataCollector().getDatabase();
		return new QualifiedNameImpl(
				database.toIdentifier( struct.catalog() ),
				database.toIdentifier( defaultSchema( struct.schema(), annotationTarget, context ) ),
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

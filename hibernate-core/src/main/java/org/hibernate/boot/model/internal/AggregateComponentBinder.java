/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Struct;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.QualifiedName;
import org.hibernate.boot.model.relational.QualifiedNameImpl;
import org.hibernate.boot.model.relational.QualifiedNameParser;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.PropertyData;
import org.hibernate.mapping.AggregateColumn;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Component;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.java.spi.EmbeddableAggregateJavaType;
import org.hibernate.type.spi.TypeConfiguration;

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
		if ( isAggregate( inferredData.getAttributeMember(), componentClassDetails, context ) ) {
			final InFlightMetadataCollector metadataCollector = context.getMetadataCollector();
			final TypeConfiguration typeConfiguration = metadataCollector.getTypeConfiguration();
			// Determine a struct name if this is a struct through some means
			final QualifiedName structQualifiedName = determineStructName( columns, inferredData, componentClassDetails, context );
			final String structName = structQualifiedName == null ? null : structQualifiedName.render();

			// We must register a special JavaType for the embeddable which can provide a recommended JdbcType
			typeConfiguration.getJavaTypeRegistry().resolveDescriptor(
					component.getComponentClass(),
					() -> new EmbeddableAggregateJavaType<>( component.getComponentClass(), structName )
			);
			component.setStructName( structQualifiedName );
			component.setStructColumnNames( determineStructAttributeNames( inferredData, componentClassDetails ) );

			// Determine the aggregate column
			final BasicValueBinder basicValueBinder =
					new BasicValueBinder( BasicValueBinder.Kind.ATTRIBUTE, component, context );
			basicValueBinder.setReturnedClassName( inferredData.getClassOrElementType().getName() );
			basicValueBinder.setColumns( columns );
			basicValueBinder.setPersistentClassName( propertyHolder.getClassName() );
			basicValueBinder.setType(
					inferredData.getAttributeMember(),
					inferredData.getPropertyType(),
					inferredData.getDeclaringClass().getName(),
					null
			);
			final BasicValue propertyValue = basicValueBinder.make();
			final AggregateColumn aggregateColumn = (AggregateColumn) propertyValue.getColumn();
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
		final int arrayTypeCode = context.getPreferredSqlTypeCodeForArray();
		switch ( arrayTypeCode ) {
			case SqlTypes.ARRAY:
				return SqlTypes.STRUCT_ARRAY;
			case SqlTypes.TABLE:
				return SqlTypes.STRUCT_TABLE;
			default:
				throw new UnsupportedOperationException( "Dialect does not support structured array types: " + context.getMetadataCollector()
						.getDatabase()
						.getDialect()
						.getClass()
						.getName() );
		}
	}

	private static QualifiedName determineStructName(
			AnnotatedColumns columns,
			PropertyData inferredData,
			ClassDetails returnedClassOrElement,
			MetadataBuildingContext context) {
		final MemberDetails property = inferredData.getAttributeMember();
		if ( property != null ) {
			final Struct struct = property.getDirectAnnotationUsage( Struct.class );
			if ( struct != null ) {
				return toQualifiedName( struct, context );
			}

			final JdbcTypeCode jdbcTypeCodeAnn = property.getDirectAnnotationUsage( JdbcTypeCode.class );
			if ( jdbcTypeCodeAnn != null
					&& ( jdbcTypeCodeAnn.value() == SqlTypes.STRUCT
					|| jdbcTypeCodeAnn.value() == SqlTypes.STRUCT_ARRAY
					|| jdbcTypeCodeAnn.value() == SqlTypes.STRUCT_TABLE )
					&& columns != null ) {
				final List<AnnotatedColumn> columnList = columns.getColumns();
				final String sqlType;
				if ( columnList.size() == 1 && ( sqlType = columnList.get( 0 ).getSqlType() ) != null ) {
					if ( sqlType.contains( "." ) ) {
						return QualifiedNameParser.INSTANCE.parse( sqlType );
					}
					return new QualifiedNameParser.NameParts(
							null,
							null,
							context.getMetadataCollector().getDatabase().toIdentifier( sqlType )
					);
				}
			}
		}

		final Struct struct = returnedClassOrElement.getDirectAnnotationUsage( Struct.class );
		if ( struct != null ) {
			return toQualifiedName( struct, context );
		}

		return null;
	}

	private static QualifiedName toQualifiedName(Struct struct, MetadataBuildingContext context) {
		final Database database = context.getMetadataCollector().getDatabase();
		return new QualifiedNameImpl(
				database.toIdentifier( struct.catalog() ),
				database.toIdentifier( struct.schema() ),
				database.toIdentifier( struct.name() )
		);
	}

	private static String[] determineStructAttributeNames(PropertyData inferredData, ClassDetails returnedClassOrElement) {
		final MemberDetails property = inferredData.getAttributeMember();
		if ( property != null ) {
			final Struct struct = property.getDirectAnnotationUsage( Struct.class );
			if ( struct != null ) {
				return struct.attributes();
			}
		}

		final Struct struct = returnedClassOrElement.getDirectAnnotationUsage( Struct.class );
		if ( struct != null ) {
			return struct.attributes();
		}

		return null;
	}

	private static boolean isAggregate(
			MemberDetails property,
			ClassDetails returnedClass,
			MetadataBuildingContext context) {
		if ( property != null ) {
			if ( property.hasDirectAnnotationUsage( Struct.class ) ) {
				return true;
			}

			final JdbcTypeCode jdbcTypeCode = property.getDirectAnnotationUsage( JdbcTypeCode.class );
			if ( jdbcTypeCode != null ) {
				switch ( jdbcTypeCode.value() ) {
					case SqlTypes.STRUCT:
					case SqlTypes.JSON:
					case SqlTypes.SQLXML:
					case SqlTypes.STRUCT_ARRAY:
					case SqlTypes.STRUCT_TABLE:
					case SqlTypes.JSON_ARRAY:
					case SqlTypes.XML_ARRAY: {
						return true;
					}
				}
			}
		}

		if ( returnedClass != null ) {
			return returnedClass.hasDirectAnnotationUsage( Struct.class );
		}

		return false;
	}
}

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.model.internal;

import java.util.List;

import org.hibernate.AnnotationException;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Struct;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.PropertyData;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.AggregateColumn;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Value;
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
			XClass componentXClass,
			AnnotatedColumns columns,
			MetadataBuildingContext context) {
		if ( isAggregate( inferredData.getProperty(), componentXClass ) ) {
			validateComponent( component, BinderHelper.getPath( propertyHolder, inferredData ) );

			final InFlightMetadataCollector metadataCollector = context.getMetadataCollector();
			final TypeConfiguration typeConfiguration = metadataCollector.getTypeConfiguration();
			// Determine a struct name if this is a struct through some means
			final String structName = determineStructName( columns, inferredData, componentXClass );

			// We must register a special JavaType for the embeddable which can provide a recommended JdbcType
			typeConfiguration.getJavaTypeRegistry().resolveDescriptor(
					component.getComponentClass(),
					() -> new EmbeddableAggregateJavaType<>( component.getComponentClass(), structName )
			);
			component.setStructName( structName );
			component.setStructColumnNames( determineStructAttributeNames( inferredData, componentXClass ) );

			// Determine the aggregate column
			BasicValueBinder basicValueBinder = new BasicValueBinder( BasicValueBinder.Kind.ATTRIBUTE, component, context );
			basicValueBinder.setPropertyName( inferredData.getPropertyName() );
			basicValueBinder.setReturnedClassName( inferredData.getPropertyClass().getName() );
			basicValueBinder.setColumns( columns );
			basicValueBinder.setPersistentClassName( propertyHolder.getClassName() );
			basicValueBinder.setType(
					inferredData.getProperty(),
					inferredData.getPropertyClass(),
					inferredData.getDeclaringClass().getName(),
					null
			);
			final BasicValue propertyValue = basicValueBinder.make();
			final AggregateColumn aggregateColumn = (AggregateColumn) propertyValue.getColumn();
			if ( structName != null && aggregateColumn.getSqlType() == null ) {
				if ( inferredData.getProperty().isArray() || inferredData.getProperty().isCollection() ) {
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
							componentXClass,
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
				throw new IllegalArgumentException( "Unsupported array type code: " + arrayTypeCode );
		}
	}

	private static void validateComponent(Component component, String basePath) {
		for ( Property property : component.getProperties() ) {
			final Value value = property.getValue();
			if ( !( value instanceof BasicValue ) && !( value instanceof Component ) ) {
				// todo: see HHH-15831
				throw new AnnotationException(
						"Property '" + StringHelper.qualify( basePath, property.getName() )
								+ "' uses not yet supported mapping type '"
								+ value.getClass().getName()
								+ "' in component class '"
								+ component.getComponentClassName()
								+ "'. Aggregate components currently may only contain basic values and components of basic values."
				);
			}
			if ( value instanceof Component ) {
				final Component c = (Component) value;
				if ( c.getAggregateColumn() == null ) {
					validateComponent( c, StringHelper.qualify( basePath, property.getName() ) );
				}
			}
		}
	}

	private static String determineStructName(
			AnnotatedColumns columns,
			PropertyData inferredData,
			XClass returnedClassOrElement) {
		final XProperty property = inferredData.getProperty();
		if ( property != null ) {
			final Struct struct = property.getAnnotation( Struct.class );
			if ( struct != null ) {
				return struct.name();
			}
			final JdbcTypeCode jdbcTypeCode = property.getAnnotation( JdbcTypeCode.class );
			if ( jdbcTypeCode != null
					&& ( jdbcTypeCode.value() == SqlTypes.STRUCT || jdbcTypeCode.value() == SqlTypes.STRUCT_ARRAY || jdbcTypeCode.value() == SqlTypes.STRUCT_TABLE )
					&& columns != null ) {
				final List<AnnotatedColumn> columnList = columns.getColumns();
				if ( columnList.size() == 1 && columnList.get( 0 ).getSqlType() != null ) {
					return columnList.get( 0 ).getSqlType();
				}
			}
		}
		final Struct struct = returnedClassOrElement.getAnnotation( Struct.class );
		if ( struct != null ) {
			return struct.name();
		}
		return null;
	}

	private static String[] determineStructAttributeNames(PropertyData inferredData, XClass returnedClassOrElement) {
		final XProperty property = inferredData.getProperty();
		if ( property != null ) {
			final Struct struct = property.getAnnotation( Struct.class );
			if ( struct != null ) {
				return struct.attributes();
			}
		}
		final Struct struct = returnedClassOrElement.getAnnotation( Struct.class );
		if ( struct != null ) {
			return struct.attributes();
		}
		return null;
	}

	private static boolean isAggregate(XProperty property, XClass returnedClass) {
		if ( property != null ) {
			final Struct struct = property.getAnnotation( Struct.class );
			if ( struct != null ) {
				return true;
			}
			final JdbcTypeCode jdbcTypeCode = property.getAnnotation( JdbcTypeCode.class );
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
		if ( returnedClass != null ) {
			return returnedClass.isAnnotationPresent( Struct.class );
		}
		return false;
	}
}

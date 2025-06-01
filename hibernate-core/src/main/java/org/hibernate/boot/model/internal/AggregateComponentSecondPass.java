/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.hibernate.AnnotationException;
import org.hibernate.MappingException;
import org.hibernate.annotations.Comment;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.model.relational.QualifiedName;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.SecondPass;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.aggregate.AggregateSupport;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.mapping.AggregateColumn;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.ToOne;
import org.hibernate.mapping.UserDefinedObjectType;
import org.hibernate.mapping.Value;
import org.hibernate.metamodel.internal.EmbeddableHelper;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.sql.Template;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.internal.util.StringHelper.qualify;

/**
 * @author Christian Beikov
 */
public class AggregateComponentSecondPass implements SecondPass {

	private final PropertyHolder propertyHolder;
	private final Component component;
	private final ClassDetails componentClassDetails;
	private final String propertyName;
	private final MetadataBuildingContext context;

	public AggregateComponentSecondPass(
			PropertyHolder propertyHolder,
			Component component,
			ClassDetails componentClassDetails,
			String propertyName,
			MetadataBuildingContext context) {
		this.propertyHolder = propertyHolder;
		this.component = component;
		this.componentClassDetails = componentClassDetails;
		this.propertyName = propertyName;
		this.context = context;
	}

	@Override
	public void doSecondPass(Map<String, PersistentClass> persistentClasses) throws MappingException {
		validateComponent( component, qualify( propertyHolder.getPath(), propertyName ), isAggregateArray() );

		final InFlightMetadataCollector metadataCollector = context.getMetadataCollector();
		final TypeConfiguration typeConfiguration = metadataCollector.getTypeConfiguration();
		final Database database = metadataCollector.getDatabase();
		final Dialect dialect = database.getDialect();
		final AggregateSupport aggregateSupport = dialect.getAggregateSupport();

		// Sort the component properties early to ensure the aggregated
		// columns respect the same order as the component's properties
		final int[] originalOrder = component.sortProperties();
		// Compute aggregated columns since we have to replace them in the table with the aggregate column
		final List<Column> aggregatedColumns = component.getAggregatedColumns();
		final AggregateColumn aggregateColumn = component.getAggregateColumn();

		ensureInitialized( metadataCollector, aggregateColumn );
		validateSupportedColumnTypes( propertyHolder.getPath(), component );

		final QualifiedName structName = component.getStructName();
		final boolean addAuxiliaryObjects;
		if ( structName != null ) {
			final Namespace namespace = database.locateNamespace(
					structName.getCatalogName(),
					structName.getSchemaName()
			);
			if ( !database.getDialect().supportsUserDefinedTypes() ) {
				throw new MappingException( "Database does not support user-defined types (remove '@Struct' annotation)" );
			}
			final UserDefinedObjectType udt =
					new UserDefinedObjectType( "orm", namespace, structName.getObjectName() );
			final Comment comment = componentClassDetails.getDirectAnnotationUsage( Comment.class );
			if ( comment != null ) {
				udt.setComment( comment.value() );
			}
			for ( org.hibernate.mapping.Column aggregatedColumn : aggregatedColumns ) {
				udt.addColumn( aggregatedColumn );
			}
			final UserDefinedObjectType registeredUdt = namespace.createUserDefinedType(
					structName.getObjectName(),
					name -> udt
			);
			if ( registeredUdt == udt ) {
				addAuxiliaryObjects = true;
				orderColumns( registeredUdt, originalOrder );
			}
			else {
				addAuxiliaryObjects = false;
				validateEqual( registeredUdt, udt );
			}
		}
		else {
			addAuxiliaryObjects = true;
		}
		final String aggregateReadTemplate = aggregateColumn.getAggregateReadExpressionTemplate( dialect );
		final String aggregateReadExpression = aggregateReadTemplate.replace(
				Template.TEMPLATE + ".",
				""
		);
		final String aggregateAssignmentExpression = aggregateColumn.getAggregateAssignmentExpressionTemplate( dialect )
				.replace( Template.TEMPLATE + ".", "" );
		if ( addAuxiliaryObjects ) {
			aggregateSupport.aggregateAuxiliaryDatabaseObjects(
					database.getDefaultNamespace(),
					aggregateReadExpression,
					aggregateColumn,
					aggregatedColumns
			).forEach( database::addAuxiliaryDatabaseObject );
		}
		// Hook for the dialect for allowing to flush the whole aggregate
		aggregateColumn.setCustomWrite(
				aggregateSupport.aggregateCustomWriteExpression(
						aggregateColumn,
						aggregatedColumns
				)
		);

		// The following determines the custom read/write expression and write expression for aggregatedColumns
		for ( org.hibernate.mapping.Column subColumn : aggregatedColumns ) {
			final String selectableExpression = subColumn.getText( dialect );
			final String customReadExpression;
			final String assignmentExpression = aggregateSupport.aggregateComponentAssignmentExpression(
					aggregateAssignmentExpression,
					selectableExpression,
					aggregateColumn,
					subColumn
			);

			if ( subColumn.getCustomReadExpression() == null ) {
				if ( subColumn.isFormula() ) {
					customReadExpression = aggregateSupport.aggregateComponentCustomReadExpression(
							subColumn.getTemplate( dialect, typeConfiguration ),
							Template.TEMPLATE + ".",
							aggregateReadTemplate,
							"",
							aggregateColumn,
							subColumn
					);
				}
				else {
					customReadExpression = aggregateSupport.aggregateComponentCustomReadExpression(
							"",
							"",
							aggregateReadTemplate,
							selectableExpression,
							aggregateColumn,
							subColumn
					);
				}
			}
			else {
				customReadExpression = aggregateSupport.aggregateComponentCustomReadExpression(
						subColumn.getCustomReadExpression(),
						Template.TEMPLATE + ".",
						aggregateReadTemplate,
						"",
						aggregateColumn,
						subColumn
				);
			}
			subColumn.setAssignmentExpression( assignmentExpression );
			subColumn.setCustomRead( customReadExpression );
		}

		propertyHolder.getTable().getColumns().removeAll( aggregatedColumns );
	}

	private static void validateComponent(Component component, String basePath, boolean inArray) {
		for ( Property property : component.getProperties() ) {
			final Value value = property.getValue();
			if ( value instanceof Component comp ) {
				validateComponent( comp, qualify( basePath, property.getName() ), inArray );
			}
			else if ( value instanceof ToOne toOne ) {
				if ( inArray && toOne.getReferencedPropertyName() != null ) {
					throw new AnnotationException(
							"Property '" + qualify( basePath, property.getName() )
									+ "' uses one-to-one mapping with mappedBy '"
									+ toOne.getReferencedPropertyName()
									+ "' in the aggregate component class '"
									+ component.getComponentClassName()
									+ "' within an array property, which is not allowed."
					);
				}
			}
			else if ( value instanceof Collection collection ) {
				if ( inArray && collection.getMappedByProperty() != null ) {
					throw new AnnotationException(
							"Property '" + qualify( basePath, property.getName() )
									+ "' uses *-to-many mapping with mappedBy '"
									+ collection.getMappedByProperty()
									+ "' in the aggregate component class '"
									+ component.getComponentClassName()
									+ "' within an array property, which is not allowed."
					);
				}
				if ( inArray && collection.getCollectionTable() != null ) {
					throw new AnnotationException(
							"Property '" + qualify( basePath, property.getName() )
									+ "' defines a collection table '"
									+ collection.getCollectionTable()
									+ "' in the aggregate component class '"
									+ component.getComponentClassName()
									+ "' within an array property, which is not allowed."
					);
				}
			}
		}
	}

	private boolean isAggregateArray() {
		return switch ( component.getAggregateColumn().getSqlTypeCode( context.getMetadataCollector() ) ) {
			case SqlTypes.STRUCT_ARRAY,
				SqlTypes.STRUCT_TABLE,
				SqlTypes.JSON_ARRAY,
				SqlTypes.XML_ARRAY,
				SqlTypes.ARRAY,
				SqlTypes.TABLE -> true;
			default -> false;
		};
	}

	private void orderColumns(UserDefinedObjectType userDefinedType, int[] originalOrder) {
		final Class<?> componentClass = component.getComponentClass();
		final String[] structColumnNames = component.getStructColumnNames();
		if ( structColumnNames == null || structColumnNames.length == 0 ) {
			final int[] propertyMappingIndex;
			if ( ReflectHelper.isRecord( componentClass ) ) {
				if ( originalOrder == null ) {
					propertyMappingIndex = null;
				}
				else {
					final String[] componentNames = ReflectHelper.getRecordComponentNames( componentClass );
					propertyMappingIndex = EmbeddableHelper.determineMappingIndex(
							component.getPropertyNames(),
							componentNames
					);
				}
			}
			else if ( component.getInstantiatorPropertyNames() != null ) {
				propertyMappingIndex = EmbeddableHelper.determineMappingIndex(
						component.getPropertyNames(),
						component.getInstantiatorPropertyNames()
				);
			}
			else {
				propertyMappingIndex = null;
			}
			final ArrayList<Column> orderedColumns = new ArrayList<>( userDefinedType.getColumnSpan() );
			if ( propertyMappingIndex == null ) {
				// If there is default ordering possible, assume alphabetical ordering
				final List<Property> properties = component.getProperties();
				for ( Property property : properties ) {
					addColumns( orderedColumns, property.getValue() );
				}
				if ( component.isPolymorphic() ) {
					addColumns( orderedColumns, component.getDiscriminator() );
				}
			}
			else {
				final List<Property> properties = component.getProperties();
				for ( final int propertyIndex : propertyMappingIndex ) {
					addColumns( orderedColumns, properties.get( propertyIndex ).getValue() );
				}
			}
			final List<Column> reorderedColumn = context.getBuildingOptions()
					.getColumnOrderingStrategy()
					.orderUserDefinedTypeColumns( userDefinedType, context.getMetadataCollector() );
			userDefinedType.reorderColumns( reorderedColumn != null ? reorderedColumn : orderedColumns );
		}
		else {
			final ArrayList<Column> orderedColumns = new ArrayList<>( userDefinedType.getColumnSpan() );
			for ( String structColumnName : structColumnNames ) {
				if ( !addColumns( orderedColumns, component, structColumnName ) ) {
					throw new MappingException( "Couldn't find column [" + structColumnName + "] that was defined in @Struct(attributes) in the component [" + component.getComponentClassName() + "]" );
				}
			}
			userDefinedType.reorderColumns( orderedColumns );
		}
	}

	private static void addColumns(ArrayList<Column> orderedColumns, Value value) {
		if ( value instanceof Component subComponent ) {
			if ( subComponent.getAggregateColumn() == null ) {
				for ( Property property : subComponent.getProperties() ) {
					addColumns( orderedColumns, property.getValue() );
				}
			}
			else {
				orderedColumns.add( subComponent.getAggregateColumn() );
			}
		}
		else {
			orderedColumns.addAll( value.getColumns() );
		}
	}

	private static boolean addColumns(ArrayList<Column> orderedColumns, Component component, String structColumnName) {
		for ( Property property : component.getProperties() ) {
			final Value value = property.getValue();
			if ( value instanceof Component subComponent ) {
				if ( subComponent.getAggregateColumn() == null ) {
					if ( addColumns( orderedColumns, subComponent, structColumnName ) ) {
						return true;
					}
				}
				else if ( structColumnName.equals( subComponent.getAggregateColumn().getName() ) ) {
					orderedColumns.add( subComponent.getAggregateColumn() );
					return true;
				}
			}
			else {
				for ( Selectable selectable : value.getSelectables() ) {
					if ( selectable instanceof Column column
							&& structColumnName.equals( column.getName() ) ) {
						orderedColumns.add( column );
						return true;
					}
				}
			}
		}
		if ( component.isPolymorphic() ) {
			final Column column = component.getDiscriminator().getColumns().get( 0 );
			if ( structColumnName.equals( column.getName() ) ) {
				orderedColumns.add( column );
				return true;
			}
		}
		return false;
	}

	private void validateSupportedColumnTypes(String basePath, Component component) {
		for ( Property property : component.getProperties() ) {
			final Value value = property.getValue();
			if ( value instanceof Component subComponent ) {
				if ( subComponent.getAggregateColumn() == null ) {
					validateSupportedColumnTypes( qualify( basePath, property.getName() ), subComponent );
				}
			}
		}
	}

	private static void ensureInitialized(
			InFlightMetadataCollector metadataCollector,
			AggregateColumn aggregateColumn) {
		ensureParentInitialized( metadataCollector, aggregateColumn );
		ensureChildrenInitialized( metadataCollector, aggregateColumn );
	}

	private static void ensureChildrenInitialized(
			InFlightMetadataCollector metadataCollector,
			AggregateColumn aggregateColumn) {
		for ( Column aggregatedColumn : aggregateColumn.getComponent().getAggregatedColumns() ) {
			// Make sure this state is initialized
			aggregatedColumn.getSqlTypeCode( metadataCollector );
			aggregatedColumn.getSqlType( metadataCollector );
			if ( aggregatedColumn instanceof AggregateColumn aggregate ) {
				ensureChildrenInitialized( metadataCollector, aggregate );
			}
		}

	}

	private static void ensureParentInitialized(
			InFlightMetadataCollector metadataCollector,
			AggregateColumn aggregateColumn) {
		do {
			// Trigger resolving of the value so that the column gets properly filled
			aggregateColumn.getValue().getType();
			// Make sure this state is initialized
			aggregateColumn.getSqlTypeCode( metadataCollector );
			aggregateColumn.getSqlType( metadataCollector );
			aggregateColumn = aggregateColumn.getComponent().getParentAggregateColumn();
		} while ( aggregateColumn != null );
	}

	private void validateEqual(UserDefinedObjectType udt1, UserDefinedObjectType udt2) {
		if ( udt1.getColumnSpan() != udt2.getColumnSpan() ) {
			throw new MappingException(
					String.format(
							"Struct [%s] is defined by multiple components %s with different number of mappings %d and %d",
							udt1.getName(),
							findComponentClasses(),
							udt1.getColumnSpan(),
							udt2.getColumnSpan()

					)
			);
		}
		final List<Column> missingColumns = new ArrayList<>();
		for ( Column column1 : udt1.getColumns() ) {
			final Column column2 = udt2.getColumn( column1 );
			if ( column2 == null ) {
				missingColumns.add( column1 );
			}
			else if ( !column1.getSqlType().equals( column2.getSqlType() ) ) {
				throw new MappingException(
						String.format(
								"Struct [%s] of class [%s] is defined by multiple components with different mappings [%s] and [%s] for column [%s]",
								udt1.getName(),
								componentClassDetails.getName(),
								column1.getSqlType(),
								column2.getSqlType(),
								column1.getCanonicalName()
						)
				);
			}
		}

		if ( !missingColumns.isEmpty() ) {
			throw new MappingException(
					String.format(
							"Struct [%s] is defined by multiple components %s but some columns are missing in [%s]: %s",
							udt1.getName(),
							findComponentClasses(),
							componentClassDetails.getName(),
							missingColumns
					)
			);
		}
	}

	private TreeSet<String> findComponentClasses() {
		final TreeSet<String> componentClasses = new TreeSet<>();
		context.getMetadataCollector().visitRegisteredComponents(
				c -> {
					if ( component.getStructName().equals( c.getStructName() ) ) {
						componentClasses.add( c.getComponentClassName() );
					}
				}
		);
		return componentClasses;
	}

}

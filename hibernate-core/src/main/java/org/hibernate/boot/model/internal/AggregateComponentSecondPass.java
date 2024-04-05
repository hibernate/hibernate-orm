/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.model.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.hibernate.MappingException;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.SecondPass;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.aggregate.AggregateSupport;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.AggregateColumn;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.UserDefinedObjectType;
import org.hibernate.mapping.Value;
import org.hibernate.metamodel.internal.EmbeddableHelper;
import org.hibernate.sql.Template;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Christian Beikov
 */
public class AggregateComponentSecondPass implements SecondPass {

	private final PropertyHolder propertyHolder;
	private final Component component;
	private final XClass componentXClass;
	private final MetadataBuildingContext context;

	public AggregateComponentSecondPass(
			PropertyHolder propertyHolder,
			Component component,
			XClass componentXClass,
			MetadataBuildingContext context) {
		this.propertyHolder = propertyHolder;
		this.component = component;
		this.componentXClass = componentXClass;
		this.context = context;
	}

	public void doSecondPass(Map<String, PersistentClass> persistentClasses) throws MappingException {
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

		ensureInitialized( metadataCollector, typeConfiguration, dialect, aggregateColumn );
		validateSupportedColumnTypes( propertyHolder.getPath(), component );

		for ( org.hibernate.mapping.Column aggregatedColumn : aggregatedColumns ) {
			// Make sure this state is initialized
			aggregatedColumn.getSqlTypeCode( metadataCollector );
			aggregatedColumn.getSqlType( metadataCollector );
		}

		final String structName = component.getStructName();
		final boolean addAuxiliaryObjects;
		if ( structName != null ) {
			final Namespace defaultNamespace = database.getDefaultNamespace();
			final Identifier udtName = Identifier.toIdentifier( structName );
			final UserDefinedObjectType udt = new UserDefinedObjectType( "orm", defaultNamespace, udtName );
			final Comment comment = componentXClass.getAnnotation( Comment.class );
			if ( comment != null ) {
				udt.setComment( comment.value() );
			}
			for ( org.hibernate.mapping.Column aggregatedColumn : aggregatedColumns ) {
				udt.addColumn( aggregatedColumn );
			}
			final UserDefinedObjectType registeredUdt = defaultNamespace.createUserDefinedType(
					udtName,
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
							subColumn.getTemplate(
									dialect,
									typeConfiguration,
									null
							),
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
		if ( value instanceof Component ) {
			final Component subComponent = (Component) value;
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
			if ( value instanceof Component ) {
				final Component subComponent = (Component) value;
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
					if ( selectable instanceof Column && structColumnName.equals( ( (Column) selectable ).getName() ) ) {
						orderedColumns.add( (Column) selectable );
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
			if ( value instanceof Component ) {
				final Component subComponent = (Component) value;
				if ( subComponent.getAggregateColumn() == null ) {
					validateSupportedColumnTypes( StringHelper.qualify( basePath, property.getName() ), subComponent );
				}
			}
		}
	}

	private static void ensureInitialized(
			InFlightMetadataCollector metadataCollector,
			TypeConfiguration typeConfiguration,
			Dialect dialect,
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
								componentXClass.getName(),
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
							componentXClass.getName(),
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

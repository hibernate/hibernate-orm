/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.hibernate.AnnotationException;
import org.hibernate.MappingException;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.aggregate.AggregateSupport;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.AggregateColumn;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.UserDefinedType;
import org.hibernate.mapping.Value;
import org.hibernate.sql.Template;
import org.hibernate.type.BasicPluralType;
import org.hibernate.type.BasicType;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Christian Beikov
 */
public class AggregateComponentSecondPass implements SecondPass {

	private final PropertyHolder propertyHolder;
	private final Component component;
	private final XClass returnedClassOrElement;
	private final MetadataBuildingContext context;

	public AggregateComponentSecondPass(
			PropertyHolder propertyHolder,
			Component component,
			XClass returnedClassOrElement,
			MetadataBuildingContext context) {
		this.propertyHolder = propertyHolder;
		this.component = component;
		this.returnedClassOrElement = returnedClassOrElement;
		this.context = context;
	}

	public void doSecondPass(Map<String, PersistentClass> persistentClasses) throws MappingException {
		final InFlightMetadataCollector metadataCollector = context.getMetadataCollector();
		final TypeConfiguration typeConfiguration = metadataCollector.getTypeConfiguration();
		final Database database = metadataCollector.getDatabase();
		final Dialect dialect = database.getDialect();
		final AggregateSupport aggregateSupport = dialect.getAggregateSupport();

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
			final UserDefinedType udt = new UserDefinedType( "orm", defaultNamespace, udtName );
			final Comment comment = returnedClassOrElement.getAnnotation( Comment.class );
			if ( comment != null ) {
				udt.setComment( comment.value() );
			}
			for ( org.hibernate.mapping.Column aggregatedColumn : aggregatedColumns ) {
				// Clone the column, since the column name will be changed later on,
				// but we don't want the DDL to be affected by that
				udt.addColumn( aggregatedColumn );
			}
			final UserDefinedType registeredUdt = defaultNamespace.createUserDefinedType(
					udtName,
					name -> udt
			);
			addAuxiliaryObjects = registeredUdt == udt;
			if ( registeredUdt != udt ) {
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

	private void validateSupportedColumnTypes(String basePath, Component component) {
		for ( Property property : component.getProperties() ) {
			final Value value = property.getValue();
			if ( value instanceof Component ) {
				final Component subComponent = (Component) value;
				if ( subComponent.getAggregateColumn() == null ) {
					validateSupportedColumnTypes( StringHelper.qualify( basePath, property.getName() ), subComponent );
				}
			}
			else if ( value instanceof BasicValue ) {
				final BasicType<?> basicType = (BasicType<?>) value.getType();
				if ( basicType instanceof BasicPluralType<?, ?> ) {
					// todo: see HHH-15862
					throw new AnnotationException(
							"Property '" + StringHelper.qualify( basePath, property.getName() )
									+ "' uses not yet supported array mapping type in component class '"
									+ component.getComponentClassName()
									+ "'. Aggregate components currently may only contain simple basic values and components of simple basic values."
					);
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

	private void validateEqual(UserDefinedType udt1, UserDefinedType udt2) {
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
								returnedClassOrElement.getName(),
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
							returnedClassOrElement.getName(),
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

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.model.internal;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.hibernate.AnnotationException;
import org.hibernate.MappingException;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.internal.util.MutableInteger;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.SimpleValue;

import org.jboss.logging.Logger;

import static org.hibernate.internal.util.collections.CollectionHelper.mapOfSize;

/**
 * @author Emmanuel Bernard
 */
public class CopyIdentifierComponentSecondPass extends FkSecondPass {
	private static final Logger log = Logger.getLogger( CopyIdentifierComponentSecondPass.class );

	private final String referencedEntityName;
	private final String propertyName;
	private final Component component;
	private final MetadataBuildingContext buildingContext;
	private final AnnotatedJoinColumns joinColumns;

	public CopyIdentifierComponentSecondPass(
			Component comp,
			String referencedEntityName, String propertyName,
			AnnotatedJoinColumns joinColumns,
			MetadataBuildingContext buildingContext) {
		super( comp, joinColumns );
		this.component = comp;
		this.referencedEntityName = referencedEntityName;
		this.propertyName = propertyName;
		this.buildingContext = buildingContext;
		this.joinColumns = joinColumns;
	}

	@Override
	public String getReferencedEntityName() {
		return referencedEntityName;
	}

	@Override
	public boolean isInPrimaryKey() {
		// This second pass is apparently only ever used to initialize composite identifiers
		return true;
	}

	@Override
	public void doSecondPass(Map<String, PersistentClass> persistentClasses) throws MappingException {
		final PersistentClass referencedPersistentClass = persistentClasses.get( referencedEntityName );
		final Component referencedComponent = getReferencedComponent( referencedPersistentClass );

		//prepare column name structure
		boolean isExplicitReference = true;
		final List<AnnotatedJoinColumn> columns = joinColumns.getJoinColumns();
		final Map<String, AnnotatedJoinColumn> columnByReferencedName = mapOfSize( columns.size() );
		for ( AnnotatedJoinColumn joinColumn : columns ) {
			if ( !joinColumn.isReferenceImplicit() ) {
				//JPA 2 requires referencedColumnNames to be case-insensitive
				columnByReferencedName.put( joinColumn.getReferencedColumn().toLowerCase(Locale.ROOT), joinColumn );
			}
		}
		//try default column orientation
		if ( columnByReferencedName.isEmpty() ) {
			isExplicitReference = false;
			for (int i = 0; i < columns.size(); i++ ) {
				columnByReferencedName.put( String.valueOf( i ), columns.get(i) );
			}
		}

		final MutableInteger index = new MutableInteger();
		for ( Property referencedProperty : referencedComponent.getProperties() ) {
			final Property property;
			if ( referencedProperty.isComposite() ) {
				property = createComponentProperty(
						isExplicitReference,
						columnByReferencedName,
						index,
						referencedProperty
				);
			}
			else {
				property = createSimpleProperty(
						referencedPersistentClass,
						isExplicitReference,
						columnByReferencedName,
						index,
						referencedProperty
				);
			}
			component.addProperty( property );
		}
	}

	private Component getReferencedComponent(PersistentClass referencedPersistentClass) {
		if ( referencedPersistentClass == null ) {
			// TODO: much better error message if this is something that can really happen!
			throw new AnnotationException( "Unknown entity name '" + referencedEntityName + "'");
		}
		final KeyValue identifier = referencedPersistentClass.getIdentifier();
		if ( !(identifier instanceof Component) ) {
			// The entity with the @MapsId annotation has a composite
			// id type, but the referenced entity has a basic-typed id.
			// Therefore, the @MapsId annotation should have specified
			// a property of the composite id that has the foreign key
			throw new AnnotationException(
					"Missing 'value' in '@MapsId' annotation of association '" + propertyName
							+ "' of entity '" + component.getOwner().getEntityName()
							+ "' with composite identifier type"
							+ " ('@MapsId' must specify a property of the '@EmbeddedId' class which has the foreign key of '"
							+ referencedEntityName + "')"
			);
		}
		return (Component) identifier;
	}

	private Property createComponentProperty(
			boolean isExplicitReference,
			Map<String, AnnotatedJoinColumn> columnByReferencedName,
			MutableInteger index,
			Property referencedProperty ) {
		Property property = new Property();
		property.setName( referencedProperty.getName() );
		//FIXME set optional?
		//property.setOptional( property.isOptional() );
		property.setPersistentClass( component.getOwner() );
		property.setPropertyAccessorName( referencedProperty.getPropertyAccessorName() );
		Component value = new Component( buildingContext, component.getOwner() );

		property.setValue( value );
		final Component referencedValue = (Component) referencedProperty.getValue();
		value.setTypeName( referencedValue.getTypeName() );
		value.setTypeParameters( referencedValue.getTypeParameters() );
		value.setComponentClassName( referencedValue.getComponentClassName() );


		for ( Property referencedComponentProperty : referencedValue.getProperties() ) {
			if ( referencedComponentProperty.isComposite() ) {
				value.addProperty( createComponentProperty(
						isExplicitReference,
						columnByReferencedName,
						index,
						referencedComponentProperty
				) );
			}
			else {
				value.addProperty( createSimpleProperty(
						referencedValue.getOwner(),
						isExplicitReference,
						columnByReferencedName,
						index,
						referencedComponentProperty
				) );
			}
		}

		return property;
	}


	private Property createSimpleProperty(
			PersistentClass referencedPersistentClass,
			boolean isExplicitReference,
			Map<String, AnnotatedJoinColumn> columnByReferencedName,
			MutableInteger index,
			Property referencedProperty ) {
		Property property = new Property();
		property.setName( referencedProperty.getName() );
		//FIXME set optional?
		//property.setOptional( property.isOptional() );
		property.setPersistentClass( component.getOwner() );
		property.setPropertyAccessorName( referencedProperty.getPropertyAccessorName() );
		SimpleValue value = new BasicValue( buildingContext, component.getTable() );
		property.setValue( value );
		final SimpleValue referencedValue = (SimpleValue) referencedProperty.getValue();
		value.copyTypeFrom( referencedValue );

		//TODO: this bit is nasty, move up to AnnotatedJoinColumns
		final AnnotatedJoinColumn firstColumn = joinColumns.getJoinColumns().get(0);
		if ( firstColumn.isNameDeferred() ) {
			firstColumn.copyReferencedStructureAndCreateDefaultJoinColumns(
					referencedPersistentClass,
					referencedValue,
					value
			);
		}
		else {
			//FIXME take care of Formula
			for ( Selectable selectable : referencedValue.getSelectables() ) {
				if ( !(selectable instanceof Column) ) {
					log.debug( "Encountered formula definition; skipping" );
					continue;
				}
				final Column column = (Column) selectable;
				final AnnotatedJoinColumn joinColumn;
				final String logicalColumnName;
				if ( isExplicitReference ) {
					logicalColumnName = column.getName();
					//JPA 2 requires referencedColumnNames to be case-insensitive
					joinColumn = columnByReferencedName.get( logicalColumnName.toLowerCase(Locale.ROOT ) );
				}
				else {
					logicalColumnName = null;
					joinColumn = columnByReferencedName.get( String.valueOf( index.get() ) );
					index.getAndIncrement();
				}
				if ( joinColumn == null && !firstColumn.isNameDeferred() ) {
					throw new AnnotationException(
							"Property '" + propertyName
									+ "' of entity '" + component.getOwner().getEntityName()
									+ "' must have a '@JoinColumn' which references the foreign key column '"
									+ logicalColumnName + "'"
					);
				}
				final String columnName = joinColumn == null || joinColumn.isNameDeferred()
						? "tata_" + column.getName()
						: joinColumn.getName();

				final Database database = buildingContext.getMetadataCollector().getDatabase();
				final String physicalName =
						buildingContext.getBuildingOptions().getPhysicalNamingStrategy()
								.toPhysicalColumnName( database.toIdentifier( columnName ), database.getJdbcEnvironment() )
								.render( database.getDialect() );
				value.addColumn( new Column( physicalName ) );
				if ( joinColumn != null ) {
					applyComponentColumnSizeValueToJoinColumn( column, joinColumn );
					joinColumn.linkWithValue( value );
				}
				column.setValue( value );
			}
		}
		return property;
	}

	private void applyComponentColumnSizeValueToJoinColumn(Column column, AnnotatedJoinColumn joinColumn) {
		final Column mappingColumn = joinColumn.getMappingColumn();
		mappingColumn.setColumnDefinition( column.getColumnDefinition() );
		mappingColumn.setLength( column.getLength() );
		mappingColumn.setPrecision( column.getPrecision() );
		mappingColumn.setScale( column.getScale() );
		mappingColumn.setArrayLength( column.getArrayLength() );
	}
}

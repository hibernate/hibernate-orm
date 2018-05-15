/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.MappingException;
import org.hibernate.boot.model.domain.PersistentAttributeMapping;
import org.hibernate.boot.model.relational.MappedColumn;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;

import org.jboss.logging.Logger;

/**
 * @author Emmanuel Bernard
 */
public class CopyIdentifierComponentSecondPass implements SecondPass {
	private static final Logger log = Logger.getLogger( CopyIdentifierComponentSecondPass.class );

	private final String referencedEntityName;
	private final Component component;
	private final MetadataBuildingContext buildingContext;
	private final Ejb3JoinColumn[] joinColumns;

	public CopyIdentifierComponentSecondPass(
			Component comp,
			String referencedEntityName,
			Ejb3JoinColumn[] joinColumns,
			MetadataBuildingContext buildingContext) {
		this.component = comp;
		this.referencedEntityName = referencedEntityName;
		this.buildingContext = buildingContext;
		this.joinColumns = joinColumns;
	}

	@SuppressWarnings({ "unchecked" })
	public void doSecondPass(Map<String, PersistentClass> persistentClasses) throws MappingException {
		final PersistentClass referencedPersistentClass = persistentClasses.get( referencedEntityName );
		// TODO better error names
		if ( referencedPersistentClass == null ) {
			throw new AnnotationException( "Unknown entity name: " + referencedEntityName );
		}
		if ( ! ( referencedPersistentClass.getIdentifier() instanceof Component ) ) {
			throw new AssertionFailure(
					"Unexpected identifier type on the referenced entity when mapping a @MapsId: "
							+ referencedEntityName
			);
		}
		final Component referencedComponent = (Component) referencedPersistentClass.getIdentifier();

		//prepare column name structure
		boolean isExplicitReference = true;
		Map<String, Ejb3JoinColumn> columnByReferencedName = new HashMap<>(joinColumns.length);
		for (Ejb3JoinColumn joinColumn : joinColumns) {
			final String referencedColumnName = joinColumn.getReferencedColumn();
			if ( referencedColumnName == null || BinderHelper.isEmptyAnnotationValue( referencedColumnName ) ) {
				break;
			}
			//JPA 2 requires referencedColumnNames to be case insensitive
			columnByReferencedName.put( referencedColumnName.toLowerCase(Locale.ROOT), joinColumn );
		}
		//try default column orientation
		final AtomicInteger index = new AtomicInteger( 0 );
		if ( columnByReferencedName.isEmpty() ) {
			isExplicitReference = false;
			for (Ejb3JoinColumn joinColumn : joinColumns) {
				columnByReferencedName.put( "" + index.get(), joinColumn );
				index.getAndIncrement();
			}
			index.set( 0 );
		}

		final List<PersistentAttributeMapping> declaredPersistentAttributes = referencedComponent.getDeclaredPersistentAttributes();
		for(PersistentAttributeMapping referencedMapping : declaredPersistentAttributes){
			if ( referencedMapping instanceof Component ) {
				Property property = createComponentProperty(
						isExplicitReference,
						columnByReferencedName,
						index,
						referencedMapping
				);
				component.addDeclaredPersistentAttribute( property );
			}
			else {
				Property property = createSimpleProperty(
						referencedPersistentClass,
						isExplicitReference,
						columnByReferencedName,
						index,
						referencedMapping
				);
				component.addDeclaredPersistentAttribute( property );
			}
		}
	}

	private Property createComponentProperty(
			boolean isExplicitReference,
			Map<String, Ejb3JoinColumn> columnByReferencedName,
			AtomicInteger index,
			PersistentAttributeMapping referencedProperty ) {
		final Property property = new Property( buildingContext );
		property.setName( referencedProperty.getName() );
		//FIXME set optional?
		//property.setOptional( property.isOptional() );
		property.setPersistentClass( component.getOwner() );
		property.setPropertyAccessorName( referencedProperty.getPropertyAccessorName() );
		final Component value = new Component( buildingContext, component.getOwner() );

		property.setValue( value );
		final Component referencedValue = (Component) referencedProperty.getValueMapping();
		value.setExplicitTypeName( referencedValue.getTypeName() );
		value.setTypeParameters( referencedValue.getTypeParameters() );
		value.setComponentClassName( referencedValue.getEmbeddableClassName() );

		final List<PersistentAttributeMapping> declaredPersistentAttributes = referencedValue.getDeclaredPersistentAttributes();
		for(PersistentAttributeMapping referencedComponentProperty : declaredPersistentAttributes){
			if ( referencedComponentProperty instanceof Component ) {
				Property componentProperty = createComponentProperty(
						isExplicitReference,
						columnByReferencedName,
						index,
						referencedComponentProperty
				);
				value.addDeclaredPersistentAttribute( componentProperty );
			}
			else {
				Property componentProperty = createSimpleProperty(
						referencedValue.getOwner(),
						isExplicitReference,
						columnByReferencedName,
						index,
						referencedComponentProperty
				);
				value.addDeclaredPersistentAttribute( componentProperty );
			}
		}

		return property;
	}


	private Property createSimpleProperty(
			PersistentClass referencedPersistentClass,
			boolean isExplicitReference,
			Map<String, Ejb3JoinColumn> columnByReferencedName,
			AtomicInteger index,
			PersistentAttributeMapping referencedProperty ) {
		final Property property = new Property( buildingContext );
		property.setName( referencedProperty.getName() );
		//FIXME set optional?
		//property.setOptional( property.isOptional() );
		property.setPersistentClass( component.getOwner() );
		property.setPropertyAccessorName( referencedProperty.getPropertyAccessorName() );
		final BasicValue value = new BasicValue( buildingContext, component.getMappedTable()
		);
		property.setValue( value );
		final BasicValue referencedValue = (BasicValue) referencedProperty.getValueMapping();
		value.setExplicitTypeName( referencedValue.getTypeName() );
		value.setTypeParameters( referencedValue.getTypeParameters() );
		final List<MappedColumn> mappedColumns = referencedValue.getMappedColumns();
		if ( joinColumns[0].isNameDeferred() ) {
			joinColumns[0].copyReferencedStructureAndCreateDefaultJoinColumns(
				referencedPersistentClass,
				mappedColumns,
				value);
		}
		else {
			mappedColumns.forEach(
					selectable -> {//FIXME take care of Formula

				if (  Column.class.isInstance( selectable ) ) {

				final Column column = (Column) selectable;
				final Ejb3JoinColumn joinColumn;
				String logicalColumnName = null;
				if ( isExplicitReference ) {
					final String columnName = column.getText(
					);
					//JPA 2 requires referencedColumnNames to be case insensitive
					joinColumn = columnByReferencedName.get( columnName.toLowerCase(Locale.ROOT ) );
				}
				else {
					joinColumn = columnByReferencedName.get( "" + index.get() );
					index.getAndIncrement();
				}
				if ( joinColumn == null && ! joinColumns[0].isNameDeferred() ) {
					throw new AnnotationException(
							isExplicitReference ?
									"Unable to find column reference in the @MapsId mapping: " + logicalColumnName :
									"Implicit column reference in the @MapsId mapping fails, try to use explicit referenceColumnNames: " + referencedEntityName
					);
				}
				final String columnName = joinColumn == null || joinColumn.isNameDeferred() ? "tata_" + column.getName() : joinColumn
						.getName();
				value.addColumn( new Column( columnName, false ) );
				if ( joinColumn != null ) {
					applyComponentColumnSizeValueToJoinColumn( column, joinColumn );joinColumn.linkWithValue( value );
				}
				if ( value.getMappedTable() != null ) {column.setTableName( value .getMappedTable().getNameIdentifier() );
							}
						}
						else {
							log.debug( "Encountered formula definition; skipping" );
						}
					}
			);
		}
		return property;
	}

	private void applyComponentColumnSizeValueToJoinColumn(Column column, Ejb3JoinColumn joinColumn) {
		Column mappingColumn = joinColumn.getMappingColumn();
		mappingColumn.setLength( column.getLength() );
		mappingColumn.setPrecision( column.getPrecision() );
		mappingColumn.setScale( column.getScale() );
	}

	public boolean dependentUpon( CopyIdentifierComponentSecondPass other ) {
		return this.referencedEntityName.equals( other.component.getOwner().getEntityName() );
	}
}

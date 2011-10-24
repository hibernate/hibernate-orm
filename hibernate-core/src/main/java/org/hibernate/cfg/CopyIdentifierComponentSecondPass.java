/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.cfg;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.MappingException;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.SimpleValue;

/**
 * @author Emmanuel Bernard
 */
public class CopyIdentifierComponentSecondPass implements SecondPass {
	private final String referencedEntityName;
	private final Component component;
	private final Mappings mappings;
	private final Ejb3JoinColumn[] joinColumns;

	public CopyIdentifierComponentSecondPass(
			Component comp,
			String referencedEntityName,
			Ejb3JoinColumn[] joinColumns,
			Mappings mappings) {
		this.component = comp;
		this.referencedEntityName = referencedEntityName;
		this.mappings = mappings;
		this.joinColumns = joinColumns;
	}

	@SuppressWarnings({ "unchecked" })
	public void doSecondPass(Map persistentClasses) throws MappingException {
		PersistentClass referencedPersistentClass = (PersistentClass) persistentClasses.get( referencedEntityName );
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
		Component referencedComponent = (Component) referencedPersistentClass.getIdentifier();
		Iterator<Property> properties = referencedComponent.getPropertyIterator();


		//prepare column name structure
		boolean isExplicitReference = true;
		Map<String, Ejb3JoinColumn> columnByReferencedName = new HashMap<String, Ejb3JoinColumn>(joinColumns.length);
		for (Ejb3JoinColumn joinColumn : joinColumns) {
			final String referencedColumnName = joinColumn.getReferencedColumn();
			if ( referencedColumnName == null || BinderHelper.isEmptyAnnotationValue( referencedColumnName ) ) {
				break;
			}
			//JPA 2 requires referencedColumnNames to be case insensitive
			columnByReferencedName.put( referencedColumnName.toLowerCase(), joinColumn );
		}
		//try default column orientation
		int index = 0;
		if ( columnByReferencedName.isEmpty() ) {
			isExplicitReference = false;
			for (Ejb3JoinColumn joinColumn : joinColumns) {
				columnByReferencedName.put( "" + index, joinColumn );
				index++;
			}
			index = 0;
		}

		while ( properties.hasNext() ) {
			Property referencedProperty = properties.next();
			if ( referencedProperty.isComposite() ) {
				throw new AssertionFailure( "Unexpected nested component on the referenced entity when mapping a @MapsId: "
						+ referencedEntityName);
			}
			else {
				Property property = new Property();
				property.setName( referencedProperty.getName() );
				property.setNodeName( referencedProperty.getNodeName() );
				//FIXME set optional?
				//property.setOptional( property.isOptional() );
				property.setPersistentClass( component.getOwner() );
				property.setPropertyAccessorName( referencedProperty.getPropertyAccessorName() );
				SimpleValue value = new SimpleValue( mappings, component.getTable() );
				property.setValue( value );
				final SimpleValue referencedValue = (SimpleValue) referencedProperty.getValue();
				value.setTypeName( referencedValue.getTypeName() );
				value.setTypeParameters( referencedValue.getTypeParameters() );
				final Iterator<Column> columns = referencedValue.getColumnIterator();

				if ( joinColumns[0].isNameDeferred() ) {
					joinColumns[0].copyReferencedStructureAndCreateDefaultJoinColumns(
						referencedPersistentClass,
						columns,
						value);
				}
				else {
					//FIXME take care of Formula
					while ( columns.hasNext() ) {
						Column column = columns.next();
						final Ejb3JoinColumn joinColumn;
						String logicalColumnName = null;
						if ( isExplicitReference ) {
							final String columnName = column.getName();
							logicalColumnName = mappings.getLogicalColumnName( columnName, referencedPersistentClass.getTable() );
							//JPA 2 requires referencedColumnNames to be case insensitive
							joinColumn = columnByReferencedName.get( logicalColumnName.toLowerCase() );
						}
						else {
							joinColumn = columnByReferencedName.get( "" + index );
							index++;
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
						value.addColumn( new Column( columnName ) );
						column.setValue( value );
					}
				}
				component.addProperty( property );
			}
		}
	}
}

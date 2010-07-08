/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
package org.hibernate.cfg.annotations;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.persistence.UniqueConstraint;

import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.annotations.Index;
import org.hibernate.util.StringHelper;
import org.hibernate.util.CollectionHelper;
import org.hibernate.cfg.BinderHelper;
import org.hibernate.cfg.Ejb3JoinColumn;
import org.hibernate.cfg.ExtendedMappings;
import org.hibernate.cfg.IndexOrUniqueKeySecondPass;
import org.hibernate.cfg.ObjectNameNormalizer;
import org.hibernate.cfg.ObjectNameSource;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.cfg.UniqueConstraintHolder;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.DependantValue;
import org.hibernate.mapping.JoinedSubclass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.ToOne;
import org.hibernate.mapping.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Table related operations
 *
 * @author Emmanuel Bernard
 */
@SuppressWarnings("unchecked")
public class TableBinder {
	//TODO move it to a getter/setter strategy
	private static Logger log = LoggerFactory.getLogger( TableBinder.class );
	private String schema;
	private String catalog;
	private String name;
	private boolean isAbstract;
	private List<UniqueConstraintHolder> uniqueConstraints;
//	private List<String[]> uniqueConstraints;
	String constraints;
	Table denormalizedSuperTable;
	ExtendedMappings mappings;
	private String ownerEntityTable;
	private String associatedEntityTable;
	private String propertyName;
	private String ownerEntity;
	private String associatedEntity;
	private boolean isJPA2ElementCollection;

	public void setSchema(String schema) {
		this.schema = schema;
	}

	public void setCatalog(String catalog) {
		this.catalog = catalog;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setAbstract(boolean anAbstract) {
		isAbstract = anAbstract;
	}

	public void setUniqueConstraints(UniqueConstraint[] uniqueConstraints) {
		this.uniqueConstraints = TableBinder.buildUniqueConstraintHolders( uniqueConstraints );
	}

	public void setConstraints(String constraints) {
		this.constraints = constraints;
	}

	public void setDenormalizedSuperTable(Table denormalizedSuperTable) {
		this.denormalizedSuperTable = denormalizedSuperTable;
	}

	public void setMappings(ExtendedMappings mappings) {
		this.mappings = mappings;
	}

	public void setJPA2ElementCollection(boolean isJPA2ElementCollection) {
		this.isJPA2ElementCollection = isJPA2ElementCollection;
	}

	private static class AssociationTableNameSource implements ObjectNameSource {
		private final String explicitName;
		private final String logicalName;

		private AssociationTableNameSource(String explicitName, String logicalName) {
			this.explicitName = explicitName;
			this.logicalName = logicalName;
		}

		public String getExplicitName() {
			return explicitName;
		}

		public String getLogicalName() {
			return logicalName;
		}
	}

	// only bind association table currently
	public Table bind() {
		//logicalName only accurate for assoc table...
		final String unquotedOwnerTable = StringHelper.unquote( ownerEntityTable );
		final String unquotedAssocTable = StringHelper.unquote( associatedEntityTable );

		//@ElementCollection use ownerEntity_property instead of the cleaner ownerTableName_property
		// ownerEntity can be null when the table name is explicitly set
		final String ownerObjectName = isJPA2ElementCollection && ownerEntity != null ?
				StringHelper.unqualify( ownerEntity ) : unquotedOwnerTable;
		final ObjectNameSource nameSource = buildNameContext( 
				ownerObjectName,
				unquotedAssocTable );

		final boolean ownerEntityTableQuoted = StringHelper.isQuoted( ownerEntityTable );
		final boolean associatedEntityTableQuoted = StringHelper.isQuoted( associatedEntityTable );
		final ObjectNameNormalizer.NamingStrategyHelper namingStrategyHelper = new ObjectNameNormalizer.NamingStrategyHelper() {
			public String determineImplicitName(NamingStrategy strategy) {

				final String strategyResult = strategy.collectionTableName(
						ownerEntity,
						ownerObjectName,
						associatedEntity,
						unquotedAssocTable,
						propertyName

				);
				return ownerEntityTableQuoted || associatedEntityTableQuoted
						? StringHelper.quote( strategyResult )
						: strategyResult;
			}

			public String handleExplicitName(NamingStrategy strategy, String name) {
				return strategy.tableName( name );
			}
		};

		return buildAndFillTable(
				schema,
				catalog,
				nameSource,
				namingStrategyHelper,
				isAbstract,
				uniqueConstraints,
				constraints,
				denormalizedSuperTable,
				mappings,
				null
		);
	}

	private ObjectNameSource buildNameContext(String unquotedOwnerTable, String unquotedAssocTable) {
		String logicalName = mappings.getNamingStrategy().logicalCollectionTableName(
				name,
				unquotedOwnerTable,
				unquotedAssocTable,
				propertyName
		);
		if ( StringHelper.isQuoted( ownerEntityTable ) || StringHelper.isQuoted( associatedEntityTable ) ) {
			logicalName = StringHelper.quote( logicalName );
		}

		return new AssociationTableNameSource( name, logicalName );
	}
 
	public static Table buildAndFillTable(
			String schema,
			String catalog,
			ObjectNameSource nameSource,
			ObjectNameNormalizer.NamingStrategyHelper namingStrategyHelper,
			boolean isAbstract,
			List<UniqueConstraintHolder> uniqueConstraints,
			String constraints,
			Table denormalizedSuperTable,
			ExtendedMappings mappings,
			String subselect) {
		schema = BinderHelper.isDefault( schema ) ? mappings.getSchemaName() : schema;
		catalog = BinderHelper.isDefault( catalog ) ? mappings.getCatalogName() : catalog;

		String realTableName = mappings.getObjectNameNormalizer().normalizeDatabaseIdentifier(
				nameSource.getExplicitName(),
				namingStrategyHelper
		);

		final Table table;
		if ( denormalizedSuperTable != null ) {
			table = mappings.addDenormalizedTable(
					schema,
					catalog,
					realTableName,
					isAbstract,
					subselect, 
					denormalizedSuperTable
			);
		}
		else {
			table = mappings.addTable(
					schema,
					catalog,
					realTableName,
					subselect, 
					isAbstract
			);
		}

		if ( uniqueConstraints != null && uniqueConstraints.size() > 0 ) {
			mappings.addUniqueConstraintHolders( table, uniqueConstraints );
		}

		if ( constraints != null ) table.addCheckConstraint( constraints );

		// logicalName is null if we are in the second pass
		final String logicalName = nameSource.getLogicalName();
		if ( logicalName != null ) {
			mappings.addTableBinding( schema, catalog, logicalName, realTableName, denormalizedSuperTable );
		}
		return table;
	}

	/**
	 *
	 * @param schema
	 * @param catalog
	 * @param realTableName
	 * @param logicalName
	 * @param isAbstract
	 * @param uniqueConstraints
	 * @param constraints
	 * @param denormalizedSuperTable
	 * @param mappings
	 * @return
	 *
	 * @deprecated Use {@link #buildAndFillTable} instead.
	 */
	@SuppressWarnings({ "JavaDoc" })
	public static Table fillTable(
			String schema, String catalog, String realTableName, String logicalName, boolean isAbstract,
			List uniqueConstraints, String constraints, Table denormalizedSuperTable, ExtendedMappings mappings
	) {
		schema = BinderHelper.isDefault( schema ) ? mappings.getSchemaName() : schema;
		catalog = BinderHelper.isDefault( catalog ) ? mappings.getCatalogName() : catalog;
		Table table;
		if ( denormalizedSuperTable != null ) {
			table = mappings.addDenormalizedTable(
					schema,
					catalog,
					realTableName,
					isAbstract,
					null, //subselect
					denormalizedSuperTable
			);
		}
		else {
			table = mappings.addTable(
					schema,
					catalog,
					realTableName,
					null, //subselect
					isAbstract
			);
		}
		if ( uniqueConstraints != null && uniqueConstraints.size() > 0 ) {
			mappings.addUniqueConstraints( table, uniqueConstraints );
		}
		if ( constraints != null ) table.addCheckConstraint( constraints );
		//logicalName is null if we are in the second pass
		if ( logicalName != null ) {
			mappings.addTableBinding( schema, catalog, logicalName, realTableName, denormalizedSuperTable );
		}
		return table;
	}

	public static void bindFk(
			PersistentClass referencedEntity, PersistentClass destinationEntity, Ejb3JoinColumn[] columns,
			SimpleValue value,
			boolean unique, ExtendedMappings mappings
	) {
		PersistentClass associatedClass;
		if ( destinationEntity != null ) {
			//overridden destination
			associatedClass = destinationEntity;
		}
		else {
			associatedClass = columns[0].getPropertyHolder() == null
					? null
					: columns[0].getPropertyHolder().getPersistentClass();
		}
		final String mappedByProperty = columns[0].getMappedBy();
		if ( StringHelper.isNotEmpty( mappedByProperty ) ) {
			/**
			 * Get the columns of the mapped-by property
			 * copy them and link the copy to the actual value
			 */
			log.debug("Retrieving property {}.{}", associatedClass.getEntityName(), mappedByProperty);

			final Property property = associatedClass.getRecursiveProperty( columns[0].getMappedBy() );
			Iterator mappedByColumns;
			if ( property.getValue() instanceof Collection ) {
				Collection collection = ( (Collection) property.getValue() );
				Value element = collection.getElement();
				if ( element == null ) {
					throw new AnnotationException(
							"Illegal use of mappedBy on both sides of the relationship: "
									+ associatedClass.getEntityName() + "." + mappedByProperty
					);
				}
				mappedByColumns = element.getColumnIterator();
			}
			else {
				mappedByColumns = property.getValue().getColumnIterator();
			}
			while ( mappedByColumns.hasNext() ) {
				Column column = (Column) mappedByColumns.next();
				columns[0].overrideFromReferencedColumnIfNecessary( column );
				columns[0].linkValueUsingAColumnCopy( column, value );
			}
		}
		else if ( columns[0].isImplicit() ) {
			/**
			 * if columns are implicit, then create the columns based on the
			 * referenced entity id columns
			 */
			Iterator idColumns;
			if ( referencedEntity instanceof JoinedSubclass ) {
				idColumns = referencedEntity.getKey().getColumnIterator();
			}
			else {
				idColumns = referencedEntity.getIdentifier().getColumnIterator();
			}
			while ( idColumns.hasNext() ) {
				Column column = (Column) idColumns.next();
				columns[0].overrideFromReferencedColumnIfNecessary( column );
				columns[0].linkValueUsingDefaultColumnNaming( column, referencedEntity, value );
			}
		}
		else {
			int fkEnum = Ejb3JoinColumn.checkReferencedColumnsType( columns, referencedEntity, mappings );

			if ( Ejb3JoinColumn.NON_PK_REFERENCE == fkEnum ) {
				String referencedPropertyName;
				if ( value instanceof ToOne ) {
					referencedPropertyName = ( (ToOne) value ).getReferencedPropertyName();
				}
				else if ( value instanceof DependantValue ) {
					String propertyName = columns[0].getPropertyName();
					if ( propertyName != null ) {
						Collection collection = (Collection) referencedEntity.getRecursiveProperty( propertyName )
								.getValue();
						referencedPropertyName = collection.getReferencedPropertyName();
					}
					else {
						throw new AnnotationException( "SecondaryTable JoinColumn cannot reference a non primary key" );
					}

				}
				else {
					throw new AssertionFailure(
							"Do a property ref on an unexpected Value type: "
									+ value.getClass().getName()
					);
				}
				if ( referencedPropertyName == null ) {
					throw new AssertionFailure(
							"No property ref found while expected"
					);
				}
				Property synthProp = referencedEntity.getRecursiveProperty( referencedPropertyName );
				if ( synthProp == null ) {
					throw new AssertionFailure(
							"Cannot find synthProp: " + referencedEntity.getEntityName() + "." + referencedPropertyName
					);
				}
				linkJoinColumnWithValueOverridingNameIfImplicit(
						referencedEntity, synthProp.getColumnIterator(), columns, value
				);

			}
			else {
				if ( Ejb3JoinColumn.NO_REFERENCE == fkEnum ) {
					//implicit case, we hope PK and FK columns are in the same order
					if ( columns.length != referencedEntity.getIdentifier().getColumnSpan() ) {
						throw new AnnotationException(
								"A Foreign key refering " + referencedEntity.getEntityName()
										+ " from " + associatedClass.getEntityName()
										+ " has the wrong number of column. should be " + referencedEntity.getIdentifier()
										.getColumnSpan()
						);
					}
					linkJoinColumnWithValueOverridingNameIfImplicit(
							referencedEntity,
							referencedEntity.getIdentifier().getColumnIterator(),
							columns,
							value
					);
				}
				else {
					//explicit referencedColumnName
					Iterator idColItr = referencedEntity.getKey().getColumnIterator();
					org.hibernate.mapping.Column col;
					Table table = referencedEntity.getTable(); //works cause the pk has to be on the primary table
					if ( !idColItr.hasNext() ) log.debug( "No column in the identifier!" );
					while ( idColItr.hasNext() ) {
						boolean match = false;
						//for each PK column, find the associated FK column.
						col = (org.hibernate.mapping.Column) idColItr.next();
						for (Ejb3JoinColumn joinCol : columns) {
							String referencedColumn = joinCol.getReferencedColumn();
							referencedColumn = mappings.getPhysicalColumnName( referencedColumn, table );
							//In JPA 2 referencedColumnName is case insensitive
							if ( referencedColumn.equalsIgnoreCase( col.getQuotedName() ) ) {
								//proper join column
								if ( joinCol.isNameDeferred() ) {
									joinCol.linkValueUsingDefaultColumnNaming(
											col, referencedEntity, value
									);
								}
								else {
									joinCol.linkWithValue( value );
								}
								joinCol.overrideFromReferencedColumnIfNecessary( col );
								match = true;
								break;
							}
						}
						if ( !match ) {
							throw new AnnotationException(
									"Column name " + col.getName() + " of "
											+ referencedEntity.getEntityName() + " not found in JoinColumns.referencedColumnName"
							);
						}
					}
				}
			}
		}
		value.createForeignKey();
		if ( unique ) {
			createUniqueConstraint( value );
		}
	}

	public static void linkJoinColumnWithValueOverridingNameIfImplicit(
			PersistentClass referencedEntity, Iterator columnIterator, Ejb3JoinColumn[] columns, SimpleValue value
	) {	
		for (Ejb3JoinColumn joinCol : columns) {
			Column synthCol = (Column) columnIterator.next();					
			if ( joinCol.isNameDeferred() ) {
				//this has to be the default value
				joinCol.linkValueUsingDefaultColumnNaming( synthCol, referencedEntity, value );
			}
			else {
				joinCol.linkWithValue( value );
				joinCol.overrideFromReferencedColumnIfNecessary( synthCol );
			}
		}
	}

	public static void createUniqueConstraint(Value value) {
		Iterator iter = value.getColumnIterator();
		ArrayList cols = new ArrayList();
		while ( iter.hasNext() ) {
			cols.add( iter.next() );
		}
		value.getTable().createUniqueKey( cols );
	}

	public static void addIndexes(Table hibTable, Index[] indexes, ExtendedMappings mappings) {
		for (Index index : indexes) {
			//no need to handle inSecondPass here since it is only called from EntityBinder
			mappings.addSecondPass(
					new IndexOrUniqueKeySecondPass( hibTable, index.name(), index.columnNames(), mappings )
			);
		}
	}

	/**
	 * @deprecated Use {@link #buildUniqueConstraintHolders} instead
	 */
	@SuppressWarnings({ "JavaDoc" })
	public static List<String[]> buildUniqueConstraints(UniqueConstraint[] constraintsArray) {
		List<String[]> result = new ArrayList<String[]>();
		if ( constraintsArray.length != 0 ) {
			for (UniqueConstraint uc : constraintsArray) {
				result.add( uc.columnNames() );
			}
		}
		return result;
	}

	/**
	 * Build a list of {@link org.hibernate.cfg.UniqueConstraintHolder} instances given a list of
	 * {@link UniqueConstraint} annotations.
	 *
	 * @param annotations The {@link UniqueConstraint} annotations.
	 *
	 * @return The built {@link org.hibernate.cfg.UniqueConstraintHolder} instances.
	 */
	public static List<UniqueConstraintHolder> buildUniqueConstraintHolders(UniqueConstraint[] annotations) {
		List<UniqueConstraintHolder> result;
		if ( annotations == null || annotations.length == 0 ) {
			result = java.util.Collections.emptyList();
		}
		else {
			result = new ArrayList<UniqueConstraintHolder>( CollectionHelper.determineProperSizing( annotations.length ) );
			for ( UniqueConstraint uc : annotations ) {
				result.add(
						new UniqueConstraintHolder()
								.setName( uc.name() )
								.setColumns( uc.columnNames() )
				);
			}
		}
		return result;
	}

	public void setDefaultName(
			String ownerEntity, String ownerEntityTable, String associatedEntity, String associatedEntityTable,
			String propertyName
	) {
		this.ownerEntity = ownerEntity;
		this.ownerEntityTable = ownerEntityTable;
		this.associatedEntity = associatedEntity;
		this.associatedEntityTable = associatedEntityTable;
		this.propertyName = propertyName;
		this.name = null;
	}
}

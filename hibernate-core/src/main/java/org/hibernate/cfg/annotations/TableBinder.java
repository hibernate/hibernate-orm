/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg.annotations;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.annotations.Index;
import org.hibernate.boot.model.naming.EntityNaming;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.ImplicitCollectionTableNameSource;
import org.hibernate.boot.model.naming.ImplicitJoinTableNameSource;
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.NamingStrategyHelper;
import org.hibernate.boot.model.source.spi.AttributePath;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.cfg.BinderHelper;
import org.hibernate.cfg.AnnotatedJoinColumn;
import org.hibernate.cfg.IndexOrUniqueKeySecondPass;
import org.hibernate.cfg.JPAIndexHolder;
import org.hibernate.cfg.ObjectNameSource;
import org.hibernate.cfg.UniqueConstraintHolder;
import org.hibernate.dialect.Dialect;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.DependantValue;
import org.hibernate.mapping.JoinedSubclass;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.SortableValue;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.ToOne;
import org.hibernate.mapping.Value;

import org.jboss.logging.Logger;

/**
 * Table related operations
 *
 * @author Emmanuel Bernard
 */
public class TableBinder {
	//TODO move it to a getter/setter strategy
	private static final CoreMessageLogger LOG = Logger.getMessageLogger( CoreMessageLogger.class, TableBinder.class.getName() );

	MetadataBuildingContext buildingContext;

	private String schema;
	private String catalog;
	private String name;
	private boolean isAbstract;
	private List<UniqueConstraintHolder> uniqueConstraints;
	//	private List<String[]> uniqueConstraints;
	String constraints;
	private String ownerEntityTable;
	private String associatedEntityTable;
	private String propertyName;
	private String ownerClassName;
	private String ownerEntity;
	private String ownerJpaEntity;
	private String associatedClassName;
	private String associatedEntity;
	private String associatedJpaEntity;
	private boolean isJPA2ElementCollection;
	private List<JPAIndexHolder> jpaIndexHolders;

	public void setBuildingContext(MetadataBuildingContext buildingContext) {
		this.buildingContext = buildingContext;
	}

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

	public void setJpaIndex(jakarta.persistence.Index[] jpaIndex){
		this.jpaIndexHolders = buildJpaIndexHolder( jpaIndex );
	}

	public void setConstraints(String constraints) {
		this.constraints = constraints;
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
		final Identifier ownerEntityTableNameIdentifier = toIdentifier( ownerEntityTable );

		//logicalName only accurate for assoc table...
		final String unquotedOwnerTable = StringHelper.unquote( ownerEntityTable );
		final String unquotedAssocTable = StringHelper.unquote( associatedEntityTable );

		final ObjectNameSource nameSource = buildNameContext();

		final boolean ownerEntityTableQuoted = StringHelper.isQuoted( ownerEntityTable );
		final boolean associatedEntityTableQuoted = StringHelper.isQuoted( associatedEntityTable );
		final NamingStrategyHelper namingStrategyHelper = new NamingStrategyHelper() {
			@Override
			public Identifier determineImplicitName(final MetadataBuildingContext buildingContext) {
				final ImplicitNamingStrategy namingStrategy = buildingContext.getBuildingOptions().getImplicitNamingStrategy();

				Identifier name;
				if ( isJPA2ElementCollection ) {
					name = namingStrategy.determineCollectionTableName(
							new ImplicitCollectionTableNameSource() {
								private final EntityNaming entityNaming = new EntityNaming() {
									@Override
									public String getClassName() {
										return ownerClassName;
									}

									@Override
									public String getEntityName() {
										return ownerEntity;
									}

									@Override
									public String getJpaEntityName() {
										return ownerJpaEntity;
									}
								};

								@Override
								public Identifier getOwningPhysicalTableName() {
									return ownerEntityTableNameIdentifier;
								}

								@Override
								public EntityNaming getOwningEntityNaming() {
									return entityNaming;
								}

								@Override
								public AttributePath getOwningAttributePath() {
									return AttributePath.parse( propertyName );
								}

								@Override
								public MetadataBuildingContext getBuildingContext() {
									return buildingContext;
								}
							}
					);
				}
				else {
					name =  namingStrategy.determineJoinTableName(
							new ImplicitJoinTableNameSource() {
								private final EntityNaming owningEntityNaming = new EntityNaming() {
									@Override
									public String getClassName() {
										return ownerClassName;
									}

									@Override
									public String getEntityName() {
										return ownerEntity;
									}

									@Override
									public String getJpaEntityName() {
										return ownerJpaEntity;
									}
								};

								private final EntityNaming nonOwningEntityNaming = new EntityNaming() {
									@Override
									public String getClassName() {
										return associatedClassName;
									}

									@Override
									public String getEntityName() {
										return associatedEntity;
									}

									@Override
									public String getJpaEntityName() {
										return associatedJpaEntity;
									}
								};

								@Override
								public String getOwningPhysicalTableName() {
									return unquotedOwnerTable;
								}

								@Override
								public EntityNaming getOwningEntityNaming() {
									return owningEntityNaming;
								}

								@Override
								public String getNonOwningPhysicalTableName() {
									return unquotedAssocTable;
								}

								@Override
								public EntityNaming getNonOwningEntityNaming() {
									return nonOwningEntityNaming;
								}

								@Override
								public AttributePath getAssociationOwningAttributePath() {
									return AttributePath.parse( propertyName );
								}

								@Override
								public MetadataBuildingContext getBuildingContext() {
									return buildingContext;
								}
							}
					);
				}

				if ( ownerEntityTableQuoted || associatedEntityTableQuoted ) {
					name = Identifier.quote( name );
				}

				return name;
			}

			@Override
			public Identifier handleExplicitName(
					String explicitName, MetadataBuildingContext buildingContext) {
				return buildingContext.getMetadataCollector().getDatabase().toIdentifier( explicitName );
			}

			@Override
			public Identifier toPhysicalName(Identifier logicalName, MetadataBuildingContext buildingContext) {
				return buildingContext.getBuildingOptions().getPhysicalNamingStrategy().toPhysicalTableName(
						logicalName,
						buildingContext.getMetadataCollector().getDatabase().getJdbcEnvironment()
				);
			}
		};

		return buildAndFillTable(
				schema,
				catalog,
				nameSource,
				namingStrategyHelper,
				isAbstract,
				uniqueConstraints,
				jpaIndexHolders,
				constraints,
				buildingContext,
				null,
				null
		);
	}

	private Identifier toIdentifier(String tableName) {
		return buildingContext.getMetadataCollector()
				.getDatabase()
				.getJdbcEnvironment()
				.getIdentifierHelper()
				.toIdentifier( tableName );
	}

	private ObjectNameSource buildNameContext() {
		if ( name != null ) {
			return new AssociationTableNameSource( name, null );
		}

		final Identifier logicalName;
		if ( isJPA2ElementCollection ) {
			logicalName	= buildingContext.getBuildingOptions().getImplicitNamingStrategy().determineCollectionTableName(
					new ImplicitCollectionTableNameSource() {
						private final EntityNaming owningEntityNaming = new EntityNaming() {
							@Override
							public String getClassName() {
								return ownerClassName;
							}

							@Override
							public String getEntityName() {
								return ownerEntity;
							}

							@Override
							public String getJpaEntityName() {
								return ownerJpaEntity;
							}
						};

						@Override
						public Identifier getOwningPhysicalTableName() {
							return toIdentifier( ownerEntityTable );
						}

						@Override
						public EntityNaming getOwningEntityNaming() {
							return owningEntityNaming;
						}

						@Override
						public AttributePath getOwningAttributePath() {
							// we don't know path on the annotations side :(
							return AttributePath.parse( propertyName );
						}

						@Override
						public MetadataBuildingContext getBuildingContext() {
							return buildingContext;
						}
					}
			);
		}
		else {
			logicalName = buildingContext.getBuildingOptions().getImplicitNamingStrategy().determineJoinTableName(
					new ImplicitJoinTableNameSource() {
						private final EntityNaming owningEntityNaming = new EntityNaming() {
							@Override
							public String getClassName() {
								return ownerClassName;
							}

							@Override
							public String getEntityName() {
								return ownerEntity;
							}

							@Override
							public String getJpaEntityName() {
								return ownerJpaEntity;
							}
						};

						private final EntityNaming nonOwningEntityNaming = new EntityNaming() {
							@Override
							public String getClassName() {
								return associatedClassName;
							}

							@Override
							public String getEntityName() {
								return associatedEntity;
							}

							@Override
							public String getJpaEntityName() {
								return associatedJpaEntity;
							}
						};

						@Override
						public String getOwningPhysicalTableName() {
							return ownerEntityTable;
						}

						@Override
						public EntityNaming getOwningEntityNaming() {
							return owningEntityNaming;
						}

						@Override
						public String getNonOwningPhysicalTableName() {
							return associatedEntityTable;
						}

						@Override
						public EntityNaming getNonOwningEntityNaming() {
							return nonOwningEntityNaming;
						}

						@Override
						public AttributePath getAssociationOwningAttributePath() {
							return AttributePath.parse( propertyName );
						}

						@Override
						public MetadataBuildingContext getBuildingContext() {
							return buildingContext;
						}
					}
			);
		}

		return new AssociationTableNameSource( name, logicalName.render() );
	}

	public static Table buildAndFillTable(
			String schema,
			String catalog,
			ObjectNameSource nameSource,
			NamingStrategyHelper namingStrategyHelper,
			boolean isAbstract,
			List<UniqueConstraintHolder> uniqueConstraints,
			List<JPAIndexHolder> jpaIndexHolders,
			String constraints,
			MetadataBuildingContext buildingContext,
			String subselect,
			InFlightMetadataCollector.EntityTableXref denormalizedSuperTableXref) {
		final Identifier logicalName;
		if ( StringHelper.isNotEmpty( nameSource.getExplicitName() ) ) {
			logicalName = namingStrategyHelper.handleExplicitName( nameSource.getExplicitName(), buildingContext );
		}
		else {
			logicalName = namingStrategyHelper.determineImplicitName( buildingContext );
		}

		return buildAndFillTable(
				schema,
				catalog,
				logicalName,
				isAbstract,
				uniqueConstraints,
				jpaIndexHolders,
				constraints,
				buildingContext,
				subselect,
				denormalizedSuperTableXref
		);
	}

	public static Table buildAndFillTable(
			String schema,
			String catalog,
			Identifier logicalName,
			boolean isAbstract,
			List<UniqueConstraintHolder> uniqueConstraints,
			List<JPAIndexHolder> jpaIndexHolders,
			String constraints,
			MetadataBuildingContext buildingContext,
			String subselect,
			InFlightMetadataCollector.EntityTableXref denormalizedSuperTableXref) {
		schema = BinderHelper.isEmptyOrNullAnnotationValue( schema )
				? null
				: schema;
		catalog = BinderHelper.isEmptyOrNullAnnotationValue( catalog )
				? null
				: catalog;

		final Table table;
		if ( denormalizedSuperTableXref != null ) {
			table = buildingContext.getMetadataCollector().addDenormalizedTable(
					schema,
					catalog,
					logicalName.render(),
					isAbstract,
					subselect,
					denormalizedSuperTableXref.getPrimaryTable(),
					buildingContext
			);
		}
		else {
			table = buildingContext.getMetadataCollector().addTable(
					schema,
					catalog,
					logicalName.render(),
					subselect,
					isAbstract,
					buildingContext
			);
		}

		if ( CollectionHelper.isNotEmpty( uniqueConstraints ) ) {
			buildingContext.getMetadataCollector().addUniqueConstraintHolders( table, uniqueConstraints );
		}

		if ( CollectionHelper.isNotEmpty( jpaIndexHolders ) ) {
			buildingContext.getMetadataCollector().addJpaIndexHolders( table, jpaIndexHolders );
		}

		if ( constraints != null ) {
			table.addCheckConstraint( constraints );
		}

		buildingContext.getMetadataCollector().addTableNameBinding( logicalName, table );

		return table;
	}

	public static void bindFk(
			PersistentClass referencedEntity,
			PersistentClass destinationEntity,
			AnnotatedJoinColumn[] columns,
			SimpleValue value,
			boolean unique,
			MetadataBuildingContext buildingContext) {
		final PersistentClass associatedClass;
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
			/*
			 * Get the columns of the mapped-by property
			 * copy them and link the copy to the actual value
			 */
			LOG.debugf( "Retrieving property %s.%s", associatedClass.getEntityName(), mappedByProperty );

			final Value propertyVal = associatedClass.getRecursiveProperty( columns[0].getMappedBy() ).getValue();
			final List<Column> mappedByColumns;
			if ( propertyVal instanceof Collection ) {
				Value element = ((Collection) propertyVal).getElement();
				if ( element == null ) {
					throw new AnnotationException(
							"Illegal use of mappedBy on both sides of the relationship: "
									+ associatedClass.getEntityName() + "." + mappedByProperty
					);
				}
				mappedByColumns = element.getColumns();
			}
			else {
				mappedByColumns = propertyVal.getColumns();
			}
			for ( Column column: mappedByColumns ) {
				columns[0].overrideFromReferencedColumnIfNecessary( column );
				columns[0].linkValueUsingAColumnCopy( column, value );
			}
		}
		else if ( columns[0].isImplicit() ) {
			/*
			 * if columns are implicit, then create the columns based on the
			 * referenced entity id columns
			 */
			List<Column> idColumns = referencedEntity instanceof JoinedSubclass
					? referencedEntity.getKey().getColumns()
					: referencedEntity.getIdentifier().getColumns();
			for ( Column column: idColumns ) {
				columns[0].linkValueUsingDefaultColumnNaming( column, referencedEntity, value );
				columns[0].overrideFromReferencedColumnIfNecessary( column );
			}
		}
		else {
			int fkEnum = AnnotatedJoinColumn.checkReferencedColumnsType( columns, referencedEntity, buildingContext );
			if ( AnnotatedJoinColumn.NON_PK_REFERENCE == fkEnum ) {
				String referencedPropertyName;
				if ( value instanceof ToOne ) {
					referencedPropertyName = ( (ToOne) value ).getReferencedPropertyName();
				}
				else if ( value instanceof DependantValue ) {
					String propertyName = columns[0].getPropertyName();
					if ( propertyName != null ) {
						Collection collection = (Collection)
								referencedEntity.getRecursiveProperty( propertyName ).getValue();
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
				Property synthProp = referencedEntity.getReferencedProperty( referencedPropertyName );
				if ( synthProp == null ) {
					throw new AssertionFailure(
							"Cannot find synthProp: " + referencedEntity.getEntityName() + "." + referencedPropertyName
					);
				}
				linkJoinColumnWithValueOverridingNameIfImplicit(
						referencedEntity, synthProp.getValue(), columns, value
				);
				if ( value instanceof SortableValue ) {
					( (SortableValue) value ).sortProperties();
				}
			}
			else {
				if ( AnnotatedJoinColumn.NO_REFERENCE == fkEnum ) {
					//implicit case, we hope PK and FK columns are in the same order
					if ( columns.length != referencedEntity.getIdentifier().getColumnSpan() ) {
						throw new AnnotationException(
								"A Foreign key referring " + referencedEntity.getEntityName()
										+ " from " + associatedClass.getEntityName()
										+ " has the wrong number of column. should be " + referencedEntity.getIdentifier()
										.getColumnSpan()
						);
					}
					linkJoinColumnWithValueOverridingNameIfImplicit(
							referencedEntity,
							referencedEntity.getIdentifier(),
							columns,
							value
					);
					if ( value instanceof SortableValue ) {
						( (SortableValue) value ).sortProperties();
					}
				}
				else {
					// Ensure the component is sorted so that we can simply set sorted to true on the to-one
					KeyValue key = referencedEntity.getKey();
					if ( key instanceof Component ) {
						( (Component) key).sortProperties();
					}
					//explicit referencedColumnName
					List<Column> idColumns = key.getColumns();
					//works cause the pk has to be on the primary table
					if ( idColumns.isEmpty() ) {
						LOG.debug( "No column in the identifier" );
					}
					final Dialect dialect = buildingContext.getMetadataCollector().getDatabase()
							.getJdbcEnvironment().getDialect();
					for ( Column col: idColumns ) {
						boolean match = false;
						//for each PK column, find the associated FK column.
						final String colName = col.getQuotedName(dialect);
						for (AnnotatedJoinColumn joinCol : columns) {
							String referencedColumn = joinCol.getReferencedColumn();
							referencedColumn = buildingContext.getMetadataCollector().getPhysicalColumnName(
									referencedEntity.getTable(),
									referencedColumn
							);
							//In JPA 2 referencedColumnName is case-insensitive
							if ( referencedColumn.equalsIgnoreCase( colName ) ) {
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

					if ( value instanceof ToOne ) {
						( (ToOne) value ).setSorted( true );
					}
				}
			}
		}
		value.createForeignKey();
		if ( unique ) {
			value.createUniqueKey();
		}
	}

	public static void linkJoinColumnWithValueOverridingNameIfImplicit(
			PersistentClass referencedEntity,
			Value value,
			AnnotatedJoinColumn[] columns,
			SimpleValue simpleValue) {
		List<Column> valueColumns = value.getColumns();
		for ( int i = 0; i < columns.length; i++ ) {
			AnnotatedJoinColumn joinCol = columns[i];
			Column synthCol = valueColumns.get(i);
			if ( joinCol.isNameDeferred() ) {
				//this has to be the default value
				joinCol.linkValueUsingDefaultColumnNaming( synthCol, referencedEntity, simpleValue );
			}
			else {
				joinCol.linkWithValue( simpleValue );
				joinCol.overrideFromReferencedColumnIfNecessary( synthCol );
			}
		}
	}

	public static void addIndexes(Table hibTable, Index[] indexes, MetadataBuildingContext buildingContext) {
		for (Index index : indexes) {
			//no need to handle inSecondPass here since it is only called from EntityBinder
			buildingContext.getMetadataCollector().addSecondPass(
					new IndexOrUniqueKeySecondPass( hibTable, index.name(), index.columnNames(), buildingContext )
			);
		}
	}

	public static void addIndexes(Table hibTable, jakarta.persistence.Index[] indexes, MetadataBuildingContext buildingContext) {
		buildingContext.getMetadataCollector().addJpaIndexHolders( hibTable, buildJpaIndexHolder( indexes ) );
	}

	public static List<JPAIndexHolder> buildJpaIndexHolder(jakarta.persistence.Index[] indexes){
		List<JPAIndexHolder> holders = new ArrayList<>( indexes.length );
		for(jakarta.persistence.Index index : indexes){
			holders.add( new JPAIndexHolder( index ) );
		}
		return holders;
	}

	/**
	 * Build a list of {@link UniqueConstraintHolder} instances given a list of
	 * {@link UniqueConstraint} annotations.
	 *
	 * @param annotations The {@link UniqueConstraint} annotations.
	 *
	 * @return The built {@link UniqueConstraintHolder} instances.
	 */
	public static List<UniqueConstraintHolder> buildUniqueConstraintHolders(UniqueConstraint[] annotations) {
		List<UniqueConstraintHolder> result;
		if ( annotations == null || annotations.length == 0 ) {
			result = java.util.Collections.emptyList();
		}
		else {
			result = CollectionHelper.arrayList( annotations.length );
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
			String ownerClassName,
			String ownerEntity,
			String ownerJpaEntity,
			String ownerEntityTable,
			String associatedClassName,
			String associatedEntity,
			String associatedJpaEntity,
			String associatedEntityTable,
			String propertyName) {
		this.ownerClassName = ownerClassName;
		this.ownerEntity = ownerEntity;
		this.ownerJpaEntity = ownerJpaEntity;
		this.ownerEntityTable = ownerEntityTable;
		this.associatedClassName = associatedClassName;
		this.associatedEntity = associatedEntity;
		this.associatedJpaEntity = associatedJpaEntity;
		this.associatedEntityTable = associatedEntityTable;
		this.propertyName = propertyName;
		this.name = null;
	}
}

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrimaryKeyJoinColumn;

import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.MappingException;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.JoinColumnOrFormula;
import org.hibernate.annotations.JoinFormula;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.boot.model.naming.EntityNaming;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.ImplicitJoinColumnNameSource;
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.ImplicitPrimaryKeyJoinColumnNameSource;
import org.hibernate.boot.model.naming.ObjectNameNormalizer;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.source.spi.AttributePath;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.Value;

import static org.hibernate.cfg.BinderHelper.findColumnOwner;
import static org.hibernate.cfg.BinderHelper.getRelativePath;
import static org.hibernate.cfg.BinderHelper.isEmptyAnnotationValue;

/**
 * A {@link jakarta.persistence.JoinColumn} annotation
 *
 * @author Emmanuel Bernard
 */
public class AnnotatedJoinColumn extends AnnotatedColumn {
	/**
	 * property name related to this column
	 */
	private String referencedColumn;
	private String mappedBy;
	//property name on the mapped by side if any
	private String mappedByPropertyName;
	//table name on the mapped by side if any
	private String mappedByTableName;
	private String mappedByEntityName;
	private String mappedByJpaEntityName;
	private boolean JPA2ElementCollection;

	public void setJPA2ElementCollection(boolean JPA2ElementCollection) {
		this.JPA2ElementCollection = JPA2ElementCollection;
	}

	// TODO hacky solution to get the information at property ref resolution
	public String getManyToManyOwnerSideEntityName() {
		return manyToManyOwnerSideEntityName;
	}

	public void setManyToManyOwnerSideEntityName(String manyToManyOwnerSideEntityName) {
		this.manyToManyOwnerSideEntityName = manyToManyOwnerSideEntityName;
	}

	private String manyToManyOwnerSideEntityName;

	public void setReferencedColumn(String referencedColumn) {
		this.referencedColumn = referencedColumn;
	}

	public String getMappedBy() {
		return mappedBy;
	}

	public void setMappedBy(String mappedBy) {
		this.mappedBy = mappedBy;
	}

	//Due to @AnnotationOverride overriding rules, I don't want the constructor to be public
	private AnnotatedJoinColumn() {
		setMappedBy("");
	}

	//Due to @AnnotationOverride overriding rules, I don't want the constructor to be public
	//TODO get rid of it and use setters
	private AnnotatedJoinColumn(
			String sqlType,
			String name,
			String comment,
			boolean nullable,
			boolean unique,
			boolean insertable,
			boolean updatable,
			String referencedColumn,
			String secondaryTable,
			Map<String, Join> joins,
			PropertyHolder propertyHolder,
			String propertyName,
			String mappedBy,
			boolean isImplicit,
			MetadataBuildingContext buildingContext) {
		super();
		setImplicit( isImplicit );
		setSqlType( sqlType );
		setLogicalColumnName( name );
		setComment( comment );
		setNullable( nullable );
		setUnique( unique );
		setInsertable( insertable );
		setUpdatable( updatable );
		setExplicitTableName( secondaryTable );
		setPropertyHolder( propertyHolder );
		setJoins( joins );
		setBuildingContext( buildingContext );
		setPropertyName( getRelativePath( propertyHolder, propertyName ) );
		bind();
		this.referencedColumn = referencedColumn;
		this.mappedBy = mappedBy;
	}

	public String getReferencedColumn() {
		return referencedColumn;
	}

	public static AnnotatedJoinColumn[] buildJoinColumnsOrFormulas(
			JoinColumnOrFormula[] anns,
			String mappedBy,
			Map<String, Join> joins,
			PropertyHolder propertyHolder,
			String propertyName,
			MetadataBuildingContext buildingContext) {
		AnnotatedJoinColumn[] joinColumns = new AnnotatedJoinColumn[anns.length];
		for (int i = 0; i < anns.length; i++) {
			JoinColumnOrFormula join = anns[i];
			JoinFormula formula = join.formula();
			if ( formula.value() != null && !formula.value().isEmpty() ) {
				joinColumns[i] = buildJoinFormula(
						formula, mappedBy, joins, propertyHolder, propertyName, buildingContext
				);
			}
			else {
				joinColumns[i] = buildJoinColumns(
						new JoinColumn[] { join.column() }, null, mappedBy, joins, propertyHolder, propertyName, buildingContext
				)[0];
			}
		}

		return joinColumns;
	}

	/**
	 * build join formula
	 */
	public static AnnotatedJoinColumn buildJoinFormula(
			JoinFormula ann,
			String mappedBy,
			Map<String, Join> joins,
			PropertyHolder propertyHolder,
			String propertyName,
			MetadataBuildingContext buildingContext) {
		AnnotatedJoinColumn formulaColumn = new AnnotatedJoinColumn();
		formulaColumn.setFormula( ann.value() );
		formulaColumn.setReferencedColumn(ann.referencedColumnName());
		formulaColumn.setBuildingContext( buildingContext );
		formulaColumn.setPropertyHolder( propertyHolder );
		formulaColumn.setJoins( joins );
		formulaColumn.setPropertyName( getRelativePath( propertyHolder, propertyName ) );
		formulaColumn.bind();
		return formulaColumn;
	}

	public static AnnotatedJoinColumn[] buildJoinColumns(
			JoinColumn[] anns,
			Comment comment,
			String mappedBy,
			Map<String, Join> joins,
			PropertyHolder propertyHolder,
			String propertyName,
			MetadataBuildingContext buildingContext) {
		return buildJoinColumnsWithDefaultColumnSuffix(
				anns, comment, mappedBy, joins, propertyHolder, propertyName, "", buildingContext
		);
	}

	public static AnnotatedJoinColumn[] buildJoinColumnsWithDefaultColumnSuffix(
			JoinColumn[] anns,
			Comment comment,
			String mappedBy,
			Map<String, Join> joins,
			PropertyHolder propertyHolder,
			String propertyName,
			String suffixForDefaultColumnName,
			MetadataBuildingContext buildingContext) {
		JoinColumn[] actualColumns = propertyHolder.getOverriddenJoinColumn(
				StringHelper.qualify( propertyHolder.getPath(), propertyName )
		);
		if ( actualColumns == null ) actualColumns = anns;
		if ( actualColumns == null || actualColumns.length == 0 ) {
			return new AnnotatedJoinColumn[] {
					buildJoinColumn(
							null,
							comment,
							mappedBy,
							joins,
							propertyHolder,
							propertyName,
							suffixForDefaultColumnName,
							buildingContext
					)
			};
		}
		else {
			int size = actualColumns.length;
			AnnotatedJoinColumn[] result = new AnnotatedJoinColumn[size];
			for (int index = 0; index < size; index++) {
				result[index] = buildJoinColumn(
						actualColumns[index],
						comment,
						mappedBy,
						joins,
						propertyHolder,
						propertyName,
						suffixForDefaultColumnName,
						buildingContext
				);
			}
			return result;
		}
	}

	/**
	 * build join column for SecondaryTables
	 */
	private static AnnotatedJoinColumn buildJoinColumn(
			JoinColumn ann,
			Comment comment,
			String mappedBy, Map<String, Join> joins,
			PropertyHolder propertyHolder,
			String propertyName,
			String suffixForDefaultColumnName,
			MetadataBuildingContext buildingContext) {
		if ( ann != null ) {
			if ( !BinderHelper.isEmptyOrNullAnnotationValue( mappedBy ) ) {
				throw new AnnotationException(
						"Association '" + getRelativePath( propertyHolder, propertyName )
								+ "' is 'mappedBy' a different entity and may not explicitly specify the '@JoinColumn'"
				);
			}
			AnnotatedJoinColumn joinColumn = new AnnotatedJoinColumn();
			joinColumn.setComment( comment != null ? comment.value() : null );
			joinColumn.setBuildingContext( buildingContext );
			joinColumn.setJoinAnnotation( ann, null );
			if ( StringHelper.isEmpty( joinColumn.getLogicalColumnName() )
				&& ! StringHelper.isEmpty( suffixForDefaultColumnName ) ) {
				joinColumn.setLogicalColumnName( propertyName + suffixForDefaultColumnName );
			}
			joinColumn.setJoins( joins );
			joinColumn.setPropertyHolder( propertyHolder );
			joinColumn.setPropertyName( getRelativePath( propertyHolder, propertyName ) );
			joinColumn.setImplicit( false );
			joinColumn.bind();
			return joinColumn;
		}
		else {
			AnnotatedJoinColumn joinColumn = new AnnotatedJoinColumn();
			joinColumn.setMappedBy( mappedBy );
			joinColumn.setJoins( joins );
			joinColumn.setPropertyHolder( propertyHolder );
			joinColumn.setPropertyName( getRelativePath( propertyHolder, propertyName ) );
			// property name + suffix is an "explicit" column name
			if ( !StringHelper.isEmpty( suffixForDefaultColumnName ) ) {
				joinColumn.setLogicalColumnName( propertyName + suffixForDefaultColumnName );
				joinColumn.setImplicit( false );
			}
			else {
				joinColumn.setImplicit( true );
			}
			joinColumn.setBuildingContext( buildingContext );
			joinColumn.bind();
			return joinColumn;
		}
	}


	// TODO default name still useful in association table
	public void setJoinAnnotation(JoinColumn annJoin, String defaultName) {
		if ( annJoin == null ) {
			setImplicit( true );
		}
		else {
			setImplicit( false );
			if ( !isEmptyAnnotationValue( annJoin.columnDefinition() ) ) {
				setSqlType( getBuildingContext().getObjectNameNormalizer().applyGlobalQuoting( annJoin.columnDefinition() ) );
			}
			if ( !isEmptyAnnotationValue( annJoin.name() ) ) {
				setLogicalColumnName( annJoin.name() );
			}
			setNullable( annJoin.nullable() );
			setUnique( annJoin.unique() );
			setInsertable( annJoin.insertable() );
			setUpdatable( annJoin.updatable() );
			setReferencedColumn( annJoin.referencedColumnName() );

			if ( isEmptyAnnotationValue( annJoin.table() ) ) {
				setExplicitTableName( "" );
			}
			else {
				final Identifier logicalIdentifier = getBuildingContext().getMetadataCollector()
						.getDatabase()
						.toIdentifier( annJoin.table() );
				final Identifier physicalIdentifier = getBuildingContext().getBuildingOptions()
						.getPhysicalNamingStrategy()
						.toPhysicalTableName( logicalIdentifier, getBuildingContext().getMetadataCollector().getDatabase().getJdbcEnvironment() );
				setExplicitTableName(
						physicalIdentifier.render( getBuildingContext().getMetadataCollector().getDatabase().getDialect() )
				);
			}
		}
	}

	/**
	 * Build JoinColumn for a JOINED hierarchy
	 */
	public static AnnotatedJoinColumn buildJoinColumn(
			PrimaryKeyJoinColumn pkJoinAnn,
			JoinColumn joinAnn,
			Value identifier,
			Map<String, Join> joins,
			PropertyHolder propertyHolder,
			MetadataBuildingContext context) {

		final String defaultName = context.getMetadataCollector().getLogicalColumnName(
				identifier.getTable(),
				identifier.getColumns().get(0).getQuotedName()
		);
		final ObjectNameNormalizer normalizer = context.getObjectNameNormalizer();

		if ( pkJoinAnn != null || joinAnn != null ) {
			String colName;
			String columnDefinition;
			String referencedColumnName;
			if ( pkJoinAnn != null ) {
				colName = pkJoinAnn.name();
				columnDefinition = pkJoinAnn.columnDefinition();
				referencedColumnName = pkJoinAnn.referencedColumnName();
			}
			else {
				colName = joinAnn.name();
				columnDefinition = joinAnn.columnDefinition();
				referencedColumnName = joinAnn.referencedColumnName();
			}
			return new AnnotatedJoinColumn(
					columnDefinition.isEmpty()
							? null
							: normalizer.toDatabaseIdentifierText( columnDefinition ),
					colName != null && colName.isEmpty()
							? normalizer.normalizeIdentifierQuotingAsString( defaultName )
							: normalizer.normalizeIdentifierQuotingAsString( colName ),
					null,
					false,
					false,
					true,
					true,
					referencedColumnName,
					null,
					joins,
					propertyHolder,
					null,
					null,
					false,
					context
			);
		}
		else {
			return new AnnotatedJoinColumn(
					null,
					normalizer.normalizeIdentifierQuotingAsString( defaultName ),
					null,
					false,
					false,
					true,
					true,
					null,
					null,
					joins,
					propertyHolder,
					null,
					null,
					true,
					context
			);
		}
	}

	/**
	 * Override persistent class on oneToMany Cases for late settings
	 * Must only be used on second level pass binding
	 */
	public void setPersistentClass(
			PersistentClass persistentClass,
			Map<String, Join> joins,
			Map<XClass, InheritanceState> inheritanceStatePerClass) {
		// TODO shouldn't we deduce the classname from the persistentClass?
		this.propertyHolder = PropertyHolderBuilder.buildPropertyHolder(
				persistentClass,
				joins,
				getBuildingContext(),
				inheritanceStatePerClass
		);
	}

	public static void checkIfJoinColumn(Object columns, PropertyHolder holder, PropertyData property) {
		if ( !( columns instanceof AnnotatedJoinColumn[] ) ) {
			throw new AnnotationException(
					"Property '" + getRelativePath( holder, property.getPropertyName() )
							+ "' is an association and may not use '@Column' to specify column mappings (use '@JoinColumn' instead)"
			);
		}
	}

	public void copyReferencedStructureAndCreateDefaultJoinColumns(
			PersistentClass referencedEntity,
			SimpleValue referencedValue,
			SimpleValue value) {
		if ( !isNameDeferred() ) {
			throw new AssertionFailure( "Building implicit column but the column is not implicit" );
		}
		for ( Column synthCol: referencedValue.getColumns() ) {
			this.linkValueUsingDefaultColumnNaming( synthCol, referencedEntity, value );
		}
		//reset for the future
		setMappingColumn( null );
	}

	public void linkValueUsingDefaultColumnNaming(
			Column referencedColumn,
			PersistentClass referencedEntity,
			SimpleValue value) {
		String logicalReferencedColumn = getBuildingContext().getMetadataCollector().getLogicalColumnName(
				referencedEntity.getTable(),
				referencedColumn.getQuotedName()
		);
		String columnName = buildDefaultColumnName( referencedEntity, logicalReferencedColumn );

		//yuk side effect on an implicit column
		setLogicalColumnName( columnName );
		setReferencedColumn( logicalReferencedColumn );
		initMappingColumn(
				columnName,
				null, referencedColumn.getLength(),
				referencedColumn.getPrecision(),
				referencedColumn.getScale(),
				getMappingColumn() != null && getMappingColumn().isNullable(),
				referencedColumn.getSqlType(),
				getMappingColumn() != null && getMappingColumn().isUnique(),
				false
		);
		linkWithValue( value );
	}

	public void addDefaultJoinColumnName(PersistentClass referencedEntity, String logicalReferencedColumn) {
		final String columnName = buildDefaultColumnName( referencedEntity, logicalReferencedColumn );
		getMappingColumn().setName( columnName );
		setLogicalColumnName( columnName );
	}

	private String buildDefaultColumnName(final PersistentClass referencedEntity, final String logicalReferencedColumn) {
		final InFlightMetadataCollector metadataCollector = getBuildingContext().getMetadataCollector();
		final Database database = metadataCollector.getDatabase();
		final ImplicitNamingStrategy implicitNamingStrategy = getBuildingContext().getBuildingOptions().getImplicitNamingStrategy();
		final PhysicalNamingStrategy physicalNamingStrategy = getBuildingContext().getBuildingOptions().getPhysicalNamingStrategy();

		boolean mappedBySide = mappedByTableName != null || mappedByPropertyName != null;
		boolean ownerSide = getPropertyName() != null;
		boolean isRefColumnQuoted = StringHelper.isQuoted( logicalReferencedColumn );

		Identifier columnIdentifier;
		if ( mappedBySide ) {
			// NOTE : While it is completely misleading here to allow for the combination
			//		of a "JPA ElementCollection" to be mappedBy, the code that uses this
			// 		class relies on this behavior for handling the inverse side of
			// 		many-to-many mappings
			columnIdentifier = implicitNamingStrategy.determineJoinColumnName(
					new ImplicitJoinColumnNameSource() {
						final AttributePath attributePath = AttributePath.parse( mappedByPropertyName );
						final ImplicitJoinColumnNameSource.Nature implicitNamingNature = getImplicitNature();

						private final EntityNaming entityNaming = new EntityNaming() {
							@Override
							public String getClassName() {
								return referencedEntity.getClassName();
							}

							@Override
							public String getEntityName() {
								return referencedEntity.getEntityName();
							}

							@Override
							public String getJpaEntityName() {
								return referencedEntity.getJpaEntityName();
							}
						};

						private final Identifier referencedTableName = database.toIdentifier( mappedByTableName );

						@Override
						public Nature getNature() {
							return implicitNamingNature;
						}

						@Override
						public EntityNaming getEntityNaming() {
							return entityNaming;
						}

						@Override
						public AttributePath getAttributePath() {
							return attributePath;
						}

						@Override
						public Identifier getReferencedTableName() {
							return referencedTableName;
						}

						@Override
						public Identifier getReferencedColumnName() {
							if ( logicalReferencedColumn != null ) {
								return database.toIdentifier( logicalReferencedColumn );
							}

							if ( mappedByEntityName == null || mappedByPropertyName == null ) {
								return null;
							}

							final Property mappedByProperty = metadataCollector.getEntityBinding( mappedByEntityName )
									.getProperty( mappedByPropertyName );
							final SimpleValue value = (SimpleValue) mappedByProperty.getValue();
							if ( value.getSelectables().isEmpty() ) {
								throw new AnnotationException(
										String.format(
												Locale.ENGLISH,
												"Association '%s' is 'mappedBy' a property '%s' of entity '%s' with no columns",
												propertyHolder.getPath(),
												mappedByPropertyName,
												mappedByEntityName
										)
								);
							}
							final Selectable selectable = value.getSelectables().get(0);
							if ( !(selectable instanceof Column) ) {
								throw new AnnotationException(
										String.format(
												Locale.ENGLISH,
												"Association '%s' is 'mappedBy' a property '%s' of entity '%s' which maps to a formula",
												mappedByPropertyName,
												propertyHolder.getPath()
										)
								);
							}
							if ( value.getSelectables().size()>1 ) {
								throw new AnnotationException(
										String.format(
												Locale.ENGLISH,
												"Association '%s' is 'mappedBy' a property '%s' of entity '%s' with multiple columns",
												mappedByPropertyName,
												propertyHolder.getPath()
										)
								);
							}
							return database.toIdentifier( ( (Column) selectable ).getQuotedName() );
						}

						@Override
						public MetadataBuildingContext getBuildingContext() {
							return AnnotatedJoinColumn.this.getBuildingContext();
						}
					}
			);

			//one element was quoted so we quote
			if ( isRefColumnQuoted || StringHelper.isQuoted( mappedByTableName ) ) {
				columnIdentifier = Identifier.quote( columnIdentifier );
			}
		}
		else if ( ownerSide ) {
			final String logicalTableName = metadataCollector.getLogicalTableName( referencedEntity.getTable() );

			columnIdentifier = implicitNamingStrategy.determineJoinColumnName(
					new ImplicitJoinColumnNameSource() {
						final ImplicitJoinColumnNameSource.Nature implicitNamingNature = getImplicitNature();

						private final EntityNaming entityNaming = new EntityNaming() {
							@Override
							public String getClassName() {
								return referencedEntity.getClassName();
							}

							@Override
							public String getEntityName() {
								return referencedEntity.getEntityName();
							}

							@Override
							public String getJpaEntityName() {
								return referencedEntity.getJpaEntityName();
							}
						};

						private final AttributePath attributePath = AttributePath.parse( getPropertyName() );
						private final Identifier referencedTableName = database.toIdentifier( logicalTableName );
						private final Identifier referencedColumnName = database.toIdentifier( logicalReferencedColumn );

						@Override
						public Nature getNature() {
							return implicitNamingNature;
						}

						@Override
						public EntityNaming getEntityNaming() {
							return entityNaming;
						}

						@Override
						public AttributePath getAttributePath() {
							return attributePath;
						}

						@Override
						public Identifier getReferencedTableName() {
							return referencedTableName;
						}

						@Override
						public Identifier getReferencedColumnName() {
							return referencedColumnName;
						}

						@Override
						public MetadataBuildingContext getBuildingContext() {
							return AnnotatedJoinColumn.this.getBuildingContext();
						}
					}
			);

			// HHH-11826 magic. See Ejb3Column and the HHH-6005 comments
			if ( columnIdentifier.getText().contains( "_collection&&element_" ) ) {
				columnIdentifier = Identifier.toIdentifier(
						columnIdentifier.getText().replace( "_collection&&element_", "_" ),
						columnIdentifier.isQuoted()
				);
			}

			//one element was quoted so we quote
			if ( isRefColumnQuoted || StringHelper.isQuoted( logicalTableName ) ) {
				columnIdentifier = Identifier.quote( columnIdentifier );
			}
		}
		else {
			final Identifier logicalTableName = database.toIdentifier(
					metadataCollector.getLogicalTableName( referencedEntity.getTable() )
			);

			// is an intra-entity hierarchy table join so copy the name by default
			columnIdentifier = implicitNamingStrategy.determinePrimaryKeyJoinColumnName(
					new ImplicitPrimaryKeyJoinColumnNameSource() {
						@Override
						public MetadataBuildingContext getBuildingContext() {
							return AnnotatedJoinColumn.this.getBuildingContext();
						}

						@Override
						public Identifier getReferencedTableName() {
							return logicalTableName;
						}

						@Override
						public Identifier getReferencedPrimaryKeyColumnName() {
							return database.toIdentifier( logicalReferencedColumn );
						}
					}
			);

			if ( !columnIdentifier.isQuoted() && ( isRefColumnQuoted || logicalTableName.isQuoted() ) ) {
				columnIdentifier = Identifier.quote( columnIdentifier );
			}
		}

		return physicalNamingStrategy.toPhysicalColumnName( columnIdentifier, database.getJdbcEnvironment() )
				.render( database.getJdbcEnvironment().getDialect() );
	}

	private ImplicitJoinColumnNameSource.Nature getImplicitNature() {
		if ( getPropertyHolder().isEntity() ) {
			return ImplicitJoinColumnNameSource.Nature.ENTITY;
		}
		else if ( JPA2ElementCollection ) {
			return ImplicitJoinColumnNameSource.Nature.ELEMENT_COLLECTION;
		}
		else {
			return ImplicitJoinColumnNameSource.Nature.ENTITY_COLLECTION;
		}
	}

	/**
	 * used for mappedBy cases
	 */
	public void linkValueUsingAColumnCopy(Column column, SimpleValue value) {
		initMappingColumn(
				//column.getName(),
				column.getQuotedName(),
				null, column.getLength(),
				column.getPrecision(),
				column.getScale(),
				getMappingColumn().isNullable(),
				column.getSqlType(),
				getMappingColumn().isUnique(),
				false //We do copy no strategy here
		);
		linkWithValue( value );
	}

	@Override
	protected void addColumnBinding(SimpleValue value) {
		if ( StringHelper.isEmpty( mappedBy ) ) {
			// was the column explicitly quoted in the mapping/annotation
			// TODO: in metamodel, we need to better split global quoting and explicit quoting w/ respect to logical names
			boolean isLogicalColumnQuoted = StringHelper.isQuoted( getLogicalColumnName() );

			final ObjectNameNormalizer nameNormalizer = getBuildingContext().getObjectNameNormalizer();
			final String logicalColumnName = nameNormalizer.normalizeIdentifierQuotingAsString( getLogicalColumnName() );
			final String referencedColumn = nameNormalizer.normalizeIdentifierQuotingAsString( getReferencedColumn() );
			final String unquotedLogColName = StringHelper.unquote( logicalColumnName );
			final String unquotedRefColumn = StringHelper.unquote( referencedColumn );

			String logicalCollectionColumnName = StringHelper.isNotEmpty( unquotedLogColName )
					? unquotedLogColName
					: getPropertyName() + '_' + unquotedRefColumn;
			logicalCollectionColumnName = getBuildingContext().getMetadataCollector()
					.getDatabase()
					.getJdbcEnvironment()
					.getIdentifierHelper()
					.toIdentifier( logicalCollectionColumnName, isLogicalColumnQuoted )
					.render();
			getBuildingContext().getMetadataCollector().addColumnNameBinding(
					value.getTable(),
					logicalCollectionColumnName,
					getMappingColumn()
			);
		}
	}

	//keep it JDK 1.4 compliant
	//implicit way
	public static final int NO_REFERENCE = 0;
	//reference to the pk in an explicit order
	public static final int PK_REFERENCE = 1;
	//reference to non pk columns
	public static final int NON_PK_REFERENCE = 2;

	public static int checkReferencedColumnsType(
			AnnotatedJoinColumn[] columns,
			PersistentClass referencedEntity,
			MetadataBuildingContext context) {
		if ( columns.length == 0 ) {
			return NO_REFERENCE; //shortcut
		}

		final Object columnOwner = findColumnOwner( referencedEntity, columns[0].getReferencedColumn(), context );
		if ( columnOwner == null ) {
			try {
				throw new MappingException(
						"A '@JoinColumn' references a column named '" + columns[0].getReferencedColumn()
								+ "' but the target entity '" + referencedEntity.getEntityName()
								+ "' has no property which maps to this column"
				);
			}
			catch (MappingException e) {
				throw new RecoverableException( e.getMessage(), e );
			}
		}
		final Table table = columnOwner instanceof PersistentClass
				? ( (PersistentClass) columnOwner ).getTable()
				: ( (Join) columnOwner ).getTable();

		final List<Selectable> keyColumns = referencedEntity.getKey().getSelectables();
		boolean explicitColumnReference = false;
		for ( AnnotatedJoinColumn column : columns ) {
			final String logicalReferencedColumnName = column.getReferencedColumn();
			if ( StringHelper.isNotEmpty( logicalReferencedColumnName ) ) {
				explicitColumnReference = true;
				if ( !keyColumns.contains( column( context, table, logicalReferencedColumnName ) ) ) {
					// we have a column which does not belong to the PK
					return NON_PK_REFERENCE;
				}
			}
		}
		if ( explicitColumnReference ) {
			// if we got to here, all the columns belong to the PK
			return keyColumns.size() == columns.length
					// we have all the PK columns
					? PK_REFERENCE
					// we have a subset of the PK columns
					: NON_PK_REFERENCE;
		}
		else {
			// there were no nonempty referencedColumnNames
			return NO_REFERENCE;
		}
	}

	private static Column column(MetadataBuildingContext context, Table table, String logicalReferencedColumnName) {
		try {
			return new Column( context.getMetadataCollector().getPhysicalColumnName( table, logicalReferencedColumnName ) );
		}
		catch (MappingException me) {
			throw new MappingException( "No column with logical name '" + logicalReferencedColumnName
					+ "' in table '" + table.getName() + "'" );
		}
	}

	/**
	 * Called to apply column definitions from the referenced FK column to this column.
	 *
	 * @param column the referenced column.
	 */
	public void overrideFromReferencedColumnIfNecessary(Column column) {
		Column mappingColumn = getMappingColumn();
		if ( mappingColumn != null ) {
			// columnDefinition can also be specified using @JoinColumn, hence we have to check
			// whether it is set or not
			if ( StringHelper.isEmpty( sqlType ) ) {
				sqlType = column.getSqlType();
				mappingColumn.setSqlType( sqlType );
			}

			// these properties can only be applied on the referenced column - we can just take them over
			mappingColumn.setLength( column.getLength() );
			mappingColumn.setPrecision( column.getPrecision() );
			mappingColumn.setScale( column.getScale() );
		}
	}

	@Override
	public void redefineColumnName(String columnName, String propertyName, boolean applyNamingStrategy) {
		super.redefineColumnName( columnName, null, applyNamingStrategy );
	}

	public static AnnotatedJoinColumn[] buildJoinTableJoinColumns(
			JoinColumn[] annJoins,
			Map<String, Join> secondaryTables,
			PropertyHolder propertyHolder,
			String propertyName,
			String mappedBy,
			MetadataBuildingContext buildingContext) {
		AnnotatedJoinColumn[] joinColumns;
		if ( annJoins == null ) {
			AnnotatedJoinColumn currentJoinColumn = new AnnotatedJoinColumn();
			currentJoinColumn.setImplicit( true );
			currentJoinColumn.setNullable( false ); //I break the spec, but it's for good
			currentJoinColumn.setPropertyHolder( propertyHolder );
			currentJoinColumn.setJoins( secondaryTables );
			currentJoinColumn.setBuildingContext( buildingContext );
			currentJoinColumn.setPropertyName( getRelativePath( propertyHolder, propertyName ) );
			currentJoinColumn.setMappedBy( mappedBy );
			currentJoinColumn.bind();
			joinColumns = new AnnotatedJoinColumn[] { currentJoinColumn };
		}
		else {
			joinColumns = new AnnotatedJoinColumn[annJoins.length];
			JoinColumn annJoin;
			int length = annJoins.length;
			for (int index = 0; index < length; index++) {
				annJoin = annJoins[index];
				AnnotatedJoinColumn currentJoinColumn = new AnnotatedJoinColumn();
				currentJoinColumn.setImplicit( true );
				currentJoinColumn.setPropertyHolder( propertyHolder );
				currentJoinColumn.setJoins( secondaryTables );
				currentJoinColumn.setBuildingContext( buildingContext );
				currentJoinColumn.setPropertyName( getRelativePath( propertyHolder, propertyName ) );
				currentJoinColumn.setMappedBy( mappedBy );
				currentJoinColumn.setJoinAnnotation( annJoin, propertyName );
				currentJoinColumn.setNullable( false ); //I break the spec, but it's for good
				//done after the annotation to override it
				currentJoinColumn.bind();
				joinColumns[index] = currentJoinColumn;
			}
		}
		return joinColumns;
	}

	public void setMappedBy(String entityName, String jpaEntityName, String logicalTableName, String mappedByProperty) {
		this.mappedByEntityName = entityName;
		this.mappedByJpaEntityName = jpaEntityName;
		this.mappedByTableName = logicalTableName;
		this.mappedByPropertyName = mappedByProperty;
	}

	@Override
	public String toString() {
		return String.format(
				"JoinColumn{logicalColumnName='%s', referencedColumn='%s', mappedBy='%s'}",
				getLogicalColumnName(), referencedColumn, mappedBy
		);
	}
}

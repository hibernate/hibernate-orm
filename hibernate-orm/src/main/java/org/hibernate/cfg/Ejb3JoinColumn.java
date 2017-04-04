/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.persistence.JoinColumn;
import javax.persistence.PrimaryKeyJoinColumn;

import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.MappingException;
import org.hibernate.annotations.JoinColumnOrFormula;
import org.hibernate.annotations.JoinColumnsOrFormulas;
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

/**
 * Wrap state of an EJB3 @JoinColumn annotation
 * and build the Hibernate column mapping element
 *
 * @author Emmanuel Bernard
 */
@SuppressWarnings("unchecked")
public class Ejb3JoinColumn extends Ejb3Column {
	/**
	 * property name repated to this column
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
	private Ejb3JoinColumn() {
		setMappedBy( BinderHelper.ANNOTATION_STRING_DEFAULT );
	}

	//Due to @AnnotationOverride overriding rules, I don't want the constructor to be public
	//TODO get rid of it and use setters
	private Ejb3JoinColumn(
			String sqlType,
			String name,
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
		setNullable( nullable );
		setUnique( unique );
		setInsertable( insertable );
		setUpdatable( updatable );
		setExplicitTableName( secondaryTable );
		setPropertyHolder( propertyHolder );
		setJoins( joins );
		setBuildingContext( buildingContext );
		setPropertyName( BinderHelper.getRelativePath( propertyHolder, propertyName ) );
		bind();
		this.referencedColumn = referencedColumn;
		this.mappedBy = mappedBy;
	}

	public String getReferencedColumn() {
		return referencedColumn;
	}

	public static Ejb3JoinColumn[] buildJoinColumnsOrFormulas(
			JoinColumnOrFormula[] anns,
			String mappedBy,
			Map<String, Join> joins,
			PropertyHolder propertyHolder,
			String propertyName,
			MetadataBuildingContext buildingContext) {
		Ejb3JoinColumn [] joinColumns = new Ejb3JoinColumn[anns.length];
		for (int i = 0; i < anns.length; i++) {
			JoinColumnOrFormula join = anns[i];
			JoinFormula formula = join.formula();
			if (formula.value() != null && !formula.value().equals("")) {
				joinColumns[i] = buildJoinFormula(
						formula, mappedBy, joins, propertyHolder, propertyName, buildingContext
				);
			}
			else {
				joinColumns[i] = buildJoinColumns(
						new JoinColumn[] { join.column() }, mappedBy, joins, propertyHolder, propertyName, buildingContext
				)[0];
			}
		}

		return joinColumns;
	}

	/**
	 * build join formula
	 */
	public static Ejb3JoinColumn buildJoinFormula(
			JoinFormula ann,
			String mappedBy,
			Map<String, Join> joins,
			PropertyHolder propertyHolder,
			String propertyName,
			MetadataBuildingContext buildingContext) {
		Ejb3JoinColumn formulaColumn = new Ejb3JoinColumn();
		formulaColumn.setFormula( ann.value() );
		formulaColumn.setReferencedColumn(ann.referencedColumnName());
		formulaColumn.setBuildingContext( buildingContext );
		formulaColumn.setPropertyHolder( propertyHolder );
		formulaColumn.setJoins( joins );
		formulaColumn.setPropertyName( BinderHelper.getRelativePath( propertyHolder, propertyName ) );
		formulaColumn.bind();
		return formulaColumn;
	}

	public static Ejb3JoinColumn[] buildJoinColumns(
			JoinColumn[] anns,
			String mappedBy,
			Map<String, Join> joins,
			PropertyHolder propertyHolder,
			String propertyName,
			MetadataBuildingContext buildingContext) {
		return buildJoinColumnsWithDefaultColumnSuffix(
				anns, mappedBy, joins, propertyHolder, propertyName, "", buildingContext
		);
	}

	public static Ejb3JoinColumn[] buildJoinColumnsWithDefaultColumnSuffix(
			JoinColumn[] anns,
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
			return new Ejb3JoinColumn[] {
					buildJoinColumn(
							null,
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
			Ejb3JoinColumn[] result = new Ejb3JoinColumn[size];
			for (int index = 0; index < size; index++) {
				result[index] = buildJoinColumn(
						actualColumns[index],
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
	private static Ejb3JoinColumn buildJoinColumn(
			JoinColumn ann,
			String mappedBy, Map<String, Join> joins,
			PropertyHolder propertyHolder,
			String propertyName,
			String suffixForDefaultColumnName,
			MetadataBuildingContext buildingContext) {
		if ( ann != null ) {
			if ( BinderHelper.isEmptyAnnotationValue( mappedBy ) ) {
				throw new AnnotationException(
						"Illegal attempt to define a @JoinColumn with a mappedBy association: "
								+ BinderHelper.getRelativePath( propertyHolder, propertyName )
				);
			}
			Ejb3JoinColumn joinColumn = new Ejb3JoinColumn();
			joinColumn.setBuildingContext( buildingContext );
			joinColumn.setJoinAnnotation( ann, null );
			if ( StringHelper.isEmpty( joinColumn.getLogicalColumnName() )
				&& ! StringHelper.isEmpty( suffixForDefaultColumnName ) ) {
				joinColumn.setLogicalColumnName( propertyName + suffixForDefaultColumnName );
			}
			joinColumn.setJoins( joins );
			joinColumn.setPropertyHolder( propertyHolder );
			joinColumn.setPropertyName( BinderHelper.getRelativePath( propertyHolder, propertyName ) );
			joinColumn.setImplicit( false );
			joinColumn.bind();
			return joinColumn;
		}
		else {
			Ejb3JoinColumn joinColumn = new Ejb3JoinColumn();
			joinColumn.setMappedBy( mappedBy );
			joinColumn.setJoins( joins );
			joinColumn.setPropertyHolder( propertyHolder );
			joinColumn.setPropertyName(
					BinderHelper.getRelativePath( propertyHolder, propertyName )
			);
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
			if ( !BinderHelper.isEmptyAnnotationValue( annJoin.columnDefinition() ) ) {
				setSqlType( getBuildingContext().getObjectNameNormalizer().applyGlobalQuoting( annJoin.columnDefinition() ) );
			}
			if ( !BinderHelper.isEmptyAnnotationValue( annJoin.name() ) ) {
				setLogicalColumnName( annJoin.name() );
			}
			setNullable( annJoin.nullable() );
			setUnique( annJoin.unique() );
			setInsertable( annJoin.insertable() );
			setUpdatable( annJoin.updatable() );
			setReferencedColumn( annJoin.referencedColumnName() );

			if ( BinderHelper.isEmptyAnnotationValue( annJoin.table() ) ) {
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
	public static Ejb3JoinColumn buildJoinColumn(
			PrimaryKeyJoinColumn pkJoinAnn,
			JoinColumn joinAnn,
			Value identifier,
			Map<String, Join> joins,
			PropertyHolder propertyHolder,
			MetadataBuildingContext context) {

		final ObjectNameNormalizer normalizer = context.getObjectNameNormalizer();

		Column col = (Column) identifier.getColumnIterator().next();
		String defaultName = context.getMetadataCollector().getLogicalColumnName(
				identifier.getTable(),
				col.getQuotedName()
		);

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

			final String sqlType;
			if ( columnDefinition.equals( "" ) ) {
				sqlType = null;
			}
			else {
				sqlType = normalizer.toDatabaseIdentifierText( columnDefinition );
			}

			final String name;
			if ( "".equals( colName ) ) {
				name = normalizer.normalizeIdentifierQuotingAsString( defaultName );
			}
			else {
				name = context.getObjectNameNormalizer().normalizeIdentifierQuotingAsString( colName );
			}
			return new Ejb3JoinColumn(
					sqlType,
					name,
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
			defaultName = context.getObjectNameNormalizer().normalizeIdentifierQuotingAsString( defaultName );
			return new Ejb3JoinColumn(
					null,
					defaultName,
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
		// TODO shouldn't we deduce the classname from the persistentclasS?
		this.propertyHolder = PropertyHolderBuilder.buildPropertyHolder(
				persistentClass,
				joins,
				getBuildingContext(),
				inheritanceStatePerClass
		);
	}

	public static void checkIfJoinColumn(Object columns, PropertyHolder holder, PropertyData property) {
		if ( !( columns instanceof Ejb3JoinColumn[] ) ) {
			throw new AnnotationException(
					"@Column cannot be used on an association property: "
							+ holder.getEntityName()
							+ "."
							+ property.getPropertyName()
			);
		}
	}



	public void copyReferencedStructureAndCreateDefaultJoinColumns(
			PersistentClass referencedEntity,
			Iterator columnIterator,
			SimpleValue value) {
		if ( !isNameDeferred() ) {
			throw new AssertionFailure( "Building implicit column but the column is not implicit" );
		}
		while ( columnIterator.hasNext() ) {
			Column synthCol = (Column) columnIterator.next();
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
				getMappingColumn() != null ? getMappingColumn().isNullable() : false,
				referencedColumn.getSqlType(),
				getMappingColumn() != null ? getMappingColumn().isUnique() : false,
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
		final Database database = getBuildingContext().getMetadataCollector().getDatabase();
		final ImplicitNamingStrategy implicitNamingStrategy = getBuildingContext().getBuildingOptions().getImplicitNamingStrategy();
		final PhysicalNamingStrategy physicalNamingStrategy = getBuildingContext().getBuildingOptions().getPhysicalNamingStrategy();

		Identifier columnIdentifier;
		boolean mappedBySide = mappedByTableName != null || mappedByPropertyName != null;
		boolean ownerSide = getPropertyName() != null;

		Boolean isRefColumnQuoted = StringHelper.isQuoted( logicalReferencedColumn );
		final String unquotedLogicalReferenceColumn = isRefColumnQuoted
				? StringHelper.unquote( logicalReferencedColumn )
				: logicalReferencedColumn;

		if ( mappedBySide ) {
			// NOTE : While it is completely misleading here to allow for the combination
			//		of a "JPA ElementCollection" to be mappedBy, the code that uses this
			// 		class relies on this behavior for handling the inverse side of
			// 		many-to-many mappings

			final AttributePath attributePath = AttributePath.parse( mappedByPropertyName );
			final ImplicitJoinColumnNameSource.Nature implicitNamingNature;
			if ( getPropertyHolder().isEntity() ) {
				implicitNamingNature = ImplicitJoinColumnNameSource.Nature.ENTITY;
			}
			else if ( JPA2ElementCollection ) {
				implicitNamingNature = ImplicitJoinColumnNameSource.Nature.ELEMENT_COLLECTION;
			}
			else {
				implicitNamingNature = ImplicitJoinColumnNameSource.Nature.ENTITY_COLLECTION;
			}

			columnIdentifier = implicitNamingStrategy.determineJoinColumnName(
					new ImplicitJoinColumnNameSource() {
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

						private final Identifier referencedTableName = getBuildingContext().getMetadataCollector()
								.getDatabase()
								.toIdentifier( mappedByTableName );

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
								return getBuildingContext().getMetadataCollector()
										.getDatabase()
										.toIdentifier( logicalReferencedColumn );
							}

							if ( mappedByEntityName == null || mappedByPropertyName == null ) {
								return null;
							}

							final PersistentClass mappedByEntityBinding = getBuildingContext().getMetadataCollector()
									.getEntityBinding( mappedByEntityName );
							final Property mappedByProperty = mappedByEntityBinding.getProperty( mappedByPropertyName );
							final SimpleValue value = (SimpleValue) mappedByProperty.getValue();
							final Iterator<Selectable> selectableValues = value.getColumnIterator();
							if ( !selectableValues.hasNext() ) {
								throw new AnnotationException(
										String.format(
												Locale.ENGLISH,
												"mapped-by [%s] defined for attribute [%s] referenced an invalid property (no columns)",
												mappedByPropertyName,
												propertyHolder.getPath()
										)
								);
							}
							final Selectable selectable = selectableValues.next();
							if ( !Column.class.isInstance( selectable ) ) {
								throw new AnnotationException(
										String.format(
												Locale.ENGLISH,
												"mapped-by [%s] defined for attribute [%s] referenced an invalid property (formula)",
												mappedByPropertyName,
												propertyHolder.getPath()
										)
								);
							}
							if ( selectableValues.hasNext() ) {
								throw new AnnotationException(
										String.format(
												Locale.ENGLISH,
												"mapped-by [%s] defined for attribute [%s] referenced an invalid property (multiple columns)",
												mappedByPropertyName,
												propertyHolder.getPath()
										)
								);
							}
							return getBuildingContext().getMetadataCollector()
									.getDatabase()
									.toIdentifier( ( (Column) selectable ).getQuotedName() );
						}

						@Override
						public MetadataBuildingContext getBuildingContext() {
							return Ejb3JoinColumn.this.getBuildingContext();
						}
					}
			);

			//one element was quoted so we quote
			if ( isRefColumnQuoted || StringHelper.isQuoted( mappedByTableName ) ) {
				columnIdentifier = Identifier.quote( columnIdentifier );
			}
		}
		else if ( ownerSide ) {
			final String logicalTableName = getBuildingContext().getMetadataCollector().getLogicalTableName(
					referencedEntity.getTable()
			);

			final ImplicitJoinColumnNameSource.Nature implicitNamingNature;
			if ( JPA2ElementCollection ) {
				implicitNamingNature = ImplicitJoinColumnNameSource.Nature.ELEMENT_COLLECTION;
			}
			else if ( getPropertyHolder().isEntity() ) {
				implicitNamingNature = ImplicitJoinColumnNameSource.Nature.ENTITY;
			}
			else {
				implicitNamingNature = ImplicitJoinColumnNameSource.Nature.ENTITY_COLLECTION;
			}

			columnIdentifier = getBuildingContext().getBuildingOptions().getImplicitNamingStrategy().determineJoinColumnName(
					new ImplicitJoinColumnNameSource() {
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
						private final Identifier referencedTableName = getBuildingContext().getMetadataCollector()
								.getDatabase()
								.toIdentifier( logicalTableName );
						private final Identifier referencedColumnName = getBuildingContext().getMetadataCollector()
								.getDatabase()
								.toIdentifier( logicalReferencedColumn );

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
							return Ejb3JoinColumn.this.getBuildingContext();
						}
					}
			);

			//one element was quoted so we quote
			if ( isRefColumnQuoted || StringHelper.isQuoted( logicalTableName ) ) {
				columnIdentifier = Identifier.quote( columnIdentifier );
			}
		}
		else {
			final Identifier logicalTableName = database.toIdentifier(
					getBuildingContext().getMetadataCollector().getLogicalTableName( referencedEntity.getTable() )
			);

			// is an intra-entity hierarchy table join so copy the name by default
			columnIdentifier = implicitNamingStrategy.determinePrimaryKeyJoinColumnName(
					new ImplicitPrimaryKeyJoinColumnNameSource() {
						@Override
						public MetadataBuildingContext getBuildingContext() {
							return Ejb3JoinColumn.this.getBuildingContext();
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
			Ejb3JoinColumn[] columns,
			PersistentClass referencedEntity,
			MetadataBuildingContext context) {
		//convenient container to find whether a column is an id one or not
		Set<Column> idColumns = new HashSet<Column>();
		Iterator idColumnsIt = referencedEntity.getKey().getColumnIterator();
		while ( idColumnsIt.hasNext() ) {
			idColumns.add( (Column) idColumnsIt.next() );
		}

		boolean isFkReferencedColumnName = false;
		boolean noReferencedColumn = true;
		//build the list of potential tables
		if ( columns.length == 0 ) return NO_REFERENCE; //shortcut
		Object columnOwner = BinderHelper.findColumnOwner(
				referencedEntity,
				columns[0].getReferencedColumn(),
				context
		);
		if ( columnOwner == null ) {
			try {
				throw new MappingException(
						"Unable to find column with logical name: "
								+ columns[0].getReferencedColumn() + " in " + referencedEntity.getTable() + " and its related "
								+ "supertables and secondary tables"
				);
			}
			catch (MappingException e) {
				throw new RecoverableException( e.getMessage(), e );
			}
		}
		Table matchingTable = columnOwner instanceof PersistentClass ?
				( (PersistentClass) columnOwner ).getTable() :
				( (Join) columnOwner ).getTable();
		//check each referenced column
		for (Ejb3JoinColumn ejb3Column : columns) {
			String logicalReferencedColumnName = ejb3Column.getReferencedColumn();
			if ( StringHelper.isNotEmpty( logicalReferencedColumnName ) ) {
				String referencedColumnName;
				try {
					referencedColumnName = context.getMetadataCollector().getPhysicalColumnName(
							matchingTable,
							logicalReferencedColumnName
					);
				}
				catch (MappingException me) {
					//rewrite the exception
					throw new MappingException(
							"Unable to find column with logical name: "
									+ logicalReferencedColumnName + " in " + matchingTable.getName()
					);
				}
				noReferencedColumn = false;
				Column refCol = new Column( referencedColumnName );
				boolean contains = idColumns.contains( refCol );
				if ( !contains ) {
					isFkReferencedColumnName = true;
					break; //we know the state
				}
			}
		}
		if ( isFkReferencedColumnName ) {
			return NON_PK_REFERENCE;
		}
		else if ( noReferencedColumn ) {
			return NO_REFERENCE;
		}
		else if ( idColumns.size() != columns.length ) {
			//reference use PK but is a subset or a superset
			return NON_PK_REFERENCE;
		}
		else {
			return PK_REFERENCE;
		}
	}

	/**
	 * Called to apply column definitions from the referenced FK column to this column.
	 *
	 * @param column the referenced column.
	 */
	public void overrideFromReferencedColumnIfNecessary(org.hibernate.mapping.Column column) {
		if (getMappingColumn() != null) {
			// columnDefinition can also be specified using @JoinColumn, hence we have to check
			// whether it is set or not
			if ( StringHelper.isEmpty( sqlType ) ) {
				sqlType = column.getSqlType();
				getMappingColumn().setSqlType( sqlType );
			}

			// these properties can only be applied on the referenced column - we can just take them over
			getMappingColumn().setLength(column.getLength());
			getMappingColumn().setPrecision(column.getPrecision());
			getMappingColumn().setScale(column.getScale());
		}
	}

	@Override
	public void redefineColumnName(String columnName, String propertyName, boolean applyNamingStrategy) {
		if ( StringHelper.isNotEmpty( columnName ) ) {
			if ( applyNamingStrategy ) {
				getMappingColumn().setName(
						getBuildingContext().getBuildingOptions().getPhysicalNamingStrategy().toPhysicalColumnName(
								getBuildingContext().getMetadataCollector().getDatabase().toIdentifier( columnName ),
								getBuildingContext().getMetadataCollector().getDatabase().getJdbcEnvironment()
						).render()
				);
			}
			else {
				getMappingColumn().setName( columnName );
			}
		}
	}

	public static Ejb3JoinColumn[] buildJoinTableJoinColumns(
			JoinColumn[] annJoins,
			Map<String, Join> secondaryTables,
			PropertyHolder propertyHolder,
			String propertyName,
			String mappedBy,
			MetadataBuildingContext buildingContext) {
		Ejb3JoinColumn[] joinColumns;
		if ( annJoins == null ) {
			Ejb3JoinColumn currentJoinColumn = new Ejb3JoinColumn();
			currentJoinColumn.setImplicit( true );
			currentJoinColumn.setNullable( false ); //I break the spec, but it's for good
			currentJoinColumn.setPropertyHolder( propertyHolder );
			currentJoinColumn.setJoins( secondaryTables );
			currentJoinColumn.setBuildingContext( buildingContext );
			currentJoinColumn.setPropertyName(
					BinderHelper.getRelativePath( propertyHolder, propertyName )
			);
			currentJoinColumn.setMappedBy( mappedBy );
			currentJoinColumn.bind();

			joinColumns = new Ejb3JoinColumn[] {
					currentJoinColumn

			};
		}
		else {
			joinColumns = new Ejb3JoinColumn[annJoins.length];
			JoinColumn annJoin;
			int length = annJoins.length;
			for (int index = 0; index < length; index++) {
				annJoin = annJoins[index];
				Ejb3JoinColumn currentJoinColumn = new Ejb3JoinColumn();
				currentJoinColumn.setImplicit( true );
				currentJoinColumn.setPropertyHolder( propertyHolder );
				currentJoinColumn.setJoins( secondaryTables );
				currentJoinColumn.setBuildingContext( buildingContext );
				currentJoinColumn.setPropertyName( BinderHelper.getRelativePath( propertyHolder, propertyName ) );
				currentJoinColumn.setMappedBy( mappedBy );
				currentJoinColumn.setJoinAnnotation( annJoin, propertyName );
				currentJoinColumn.setNullable( false ); //I break the spec, but it's for good
				//done afterQuery the annotation to override it
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
		final StringBuilder sb = new StringBuilder();
		sb.append( "Ejb3JoinColumn" );
		sb.append( "{logicalColumnName='" ).append( getLogicalColumnName() ).append( '\'' );
		sb.append( ", referencedColumn='" ).append( referencedColumn ).append( '\'' );
		sb.append( ", mappedBy='" ).append( mappedBy ).append( '\'' );
		sb.append( '}' );
		return sb.toString();
	}
}

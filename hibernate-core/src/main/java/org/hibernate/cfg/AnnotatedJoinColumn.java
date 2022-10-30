/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg;

import java.util.List;
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
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.ObjectNameNormalizer;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.Value;

import static org.hibernate.cfg.BinderHelper.findReferencedColumnOwner;
import static org.hibernate.cfg.BinderHelper.getRelativePath;
import static org.hibernate.cfg.BinderHelper.isEmptyAnnotationValue;
import static org.hibernate.cfg.BinderHelper.isEmptyOrNullAnnotationValue;
import static org.hibernate.cfg.PropertyHolderBuilder.buildPropertyHolder;
import static org.hibernate.internal.util.StringHelper.*;

/**
 * An element of a join condition, logically representing a
 * {@link jakarta.persistence.JoinColumn} annotation, but not
 * every instance corresponds to an explicit annotation in the
 * Java code.
 *
 * @author Emmanuel Bernard
 */
public class AnnotatedJoinColumn extends AnnotatedColumn {

	private String referencedColumn;
	private AnnotatedJoinColumns parent;

	// due to @AnnotationOverride overriding rules,
	// we don't want the constructor to be public
	private AnnotatedJoinColumn() {}

	public void setReferencedColumn(String referencedColumn) {
		this.referencedColumn = referencedColumn;
	}

	/**
	 * The {@link JoinColumn#referencedColumnName() referencedColumnName}.
	 */
	public String getReferencedColumn() {
		return referencedColumn;
	}

	/**
	 * @return true if the {@code @JoinColumn} annotation did not specify the
	 *         {@link JoinColumn#referencedColumnName() referencedColumnName}.
	 */
	public boolean isReferenceImplicit() {
		return isEmptyOrNullAnnotationValue( referencedColumn );
	}

	public static AnnotatedJoinColumns buildJoinColumnsOrFormulas(
			JoinColumnOrFormula[] joinColumnOrFormulas,
			String mappedBy,
			Map<String, Join> joins,
			PropertyHolder propertyHolder,
			String propertyName,
			MetadataBuildingContext context) {
		final AnnotatedJoinColumn[] joinColumns = new AnnotatedJoinColumn[joinColumnOrFormulas.length];
		for ( int i = 0; i < joinColumnOrFormulas.length; i++ ) {
			final JoinColumnOrFormula columnOrFormula = joinColumnOrFormulas[i];
			final JoinFormula formula = columnOrFormula.formula();
			final JoinColumn column = columnOrFormula.column();
			joinColumns[i] = formula.value() != null && !formula.value().isEmpty()
					? buildJoinFormula( formula, joins, propertyHolder, propertyName, context )
					: buildJoinColumn( mappedBy, joins, propertyHolder, propertyName, context, column );
		}
		return AnnotatedJoinColumns.fromColumns( joinColumns, mappedBy, propertyHolder, context );
	}

	private static AnnotatedJoinColumn buildJoinColumn(
			String mappedBy,
			Map<String, Join> joins,
			PropertyHolder propertyHolder,
			String propertyName, MetadataBuildingContext buildingContext,
			JoinColumn joinColumn) {
		final String path = path( propertyHolder, propertyName );
		final JoinColumn[] overriddes = propertyHolder.getOverriddenJoinColumn( path );
		if ( overriddes != null ) {
			//TODO: relax this restriction
			throw new AnnotationException("Property '" + path
					+ "' overrides mapping specified using '@JoinColumnOrFormula'");
		}
		return buildJoinColumn(
				joinColumn,
				null,
				mappedBy,
				joins,
				propertyHolder,
				propertyName,
				"",
				buildingContext
		);
	}

	/**
	 * build join formula
	 */
	public static AnnotatedJoinColumn buildJoinFormula(
			JoinFormula joinFormula,
			Map<String, Join> joins,
			PropertyHolder propertyHolder,
			String propertyName,
			MetadataBuildingContext buildingContext) {
		final AnnotatedJoinColumn formulaColumn = new AnnotatedJoinColumn();
		formulaColumn.setFormula( joinFormula.value() );
		formulaColumn.setReferencedColumn( joinFormula.referencedColumnName() );
		formulaColumn.setBuildingContext( buildingContext );
		formulaColumn.setPropertyHolder( propertyHolder );
		formulaColumn.setPropertyName( getRelativePath( propertyHolder, propertyName ) );
		formulaColumn.setJoins( joins );
		formulaColumn.bind();
		return formulaColumn;
	}

	public static AnnotatedJoinColumns buildJoinColumns(
			JoinColumn[] joinColumns,
			Comment comment,
			String mappedBy,
			Map<String, Join> joins,
			PropertyHolder propertyHolder,
			String propertyName,
			MetadataBuildingContext buildingContext) {
		return buildJoinColumnsWithDefaultColumnSuffix(
				joinColumns,
				comment,
				mappedBy,
				joins,
				propertyHolder,
				propertyName,
				"",
				buildingContext
		);
	}

	public static AnnotatedJoinColumns buildJoinColumnsWithDefaultColumnSuffix(
			JoinColumn[] joinColumns,
			Comment comment,
			String mappedBy,
			Map<String, Join> joins,
			PropertyHolder propertyHolder,
			String propertyName,
			String defaultColumnSuffix,
			MetadataBuildingContext context) {
		final JoinColumn[] overriddes = propertyHolder.getOverriddenJoinColumn( path( propertyHolder, propertyName ) );
		final JoinColumn[] actualColumns = overriddes == null ? joinColumns : overriddes;
		if ( actualColumns == null || actualColumns.length == 0 ) {
			return AnnotatedJoinColumns.fromColumns( new AnnotatedJoinColumn[] {
					buildJoinColumn(
							null,
							comment,
							mappedBy,
							joins,
							propertyHolder,
							propertyName,
							defaultColumnSuffix,
							context
					)
			}, mappedBy, propertyHolder, context );
		}
		else {
			final AnnotatedJoinColumn[] result = new AnnotatedJoinColumn[actualColumns.length];
			for ( int index = 0; index < actualColumns.length; index++ ) {
				result[index] = buildJoinColumn(
						actualColumns[index],
						comment,
						mappedBy,
						joins,
						propertyHolder,
						propertyName,
						defaultColumnSuffix,
						context
				);
			}
			return AnnotatedJoinColumns.fromColumns( result, mappedBy, propertyHolder, context );
		}
	}

	private static String path(PropertyHolder propertyHolder, String propertyName) {
		return qualify( propertyHolder.getPath(), propertyName );
	}

	/**
	 * build join column for SecondaryTables
	 */
	private static AnnotatedJoinColumn buildJoinColumn(
			JoinColumn joinColumn,
			Comment comment,
			String mappedBy,
			Map<String, Join> joins,
			PropertyHolder propertyHolder,
			String propertyName,
			String defaultColumnSuffix,
			MetadataBuildingContext context) {
		if ( joinColumn != null ) {
			if ( !isEmptyOrNullAnnotationValue( mappedBy ) ) {
				throw new AnnotationException(
						"Association '" + getRelativePath( propertyHolder, propertyName )
								+ "' is 'mappedBy' a different entity and may not explicitly specify the '@JoinColumn'"
				);
			}
			return explicitJoinColumn( joinColumn, comment, joins, propertyHolder, propertyName, defaultColumnSuffix, context );
		}
		else {
			return implicitJoinColumn( joins, propertyHolder, propertyName, defaultColumnSuffix, context );
		}
	}

	private static AnnotatedJoinColumn explicitJoinColumn(
			JoinColumn joinColumn,
			Comment comment,
			Map<String, Join> joins,
			PropertyHolder propertyHolder,
			String propertyName,
			String defaultColumnSuffix,
			MetadataBuildingContext context) {
		final AnnotatedJoinColumn column = new AnnotatedJoinColumn();
		column.setComment( comment != null ? comment.value() : null );
		column.setBuildingContext( context );
		column.setJoinAnnotation(joinColumn, null );
		if ( isEmpty( column.getLogicalColumnName() ) && isNotEmpty( defaultColumnSuffix ) ) {
			column.setLogicalColumnName( propertyName + defaultColumnSuffix );
		}
		column.setJoins( joins );
		column.setPropertyHolder( propertyHolder );
		column.setPropertyName( getRelativePath( propertyHolder, propertyName ) );
		column.setImplicit( false );
		column.bind();
		return column;
	}

	private static AnnotatedJoinColumn implicitJoinColumn(
			Map<String, Join> joins,
			PropertyHolder propertyHolder,
			String propertyName,
			String defaultColumnSuffix,
			MetadataBuildingContext context) {
		final AnnotatedJoinColumn column = new AnnotatedJoinColumn();
		column.setJoins( joins );
		column.setPropertyHolder( propertyHolder );
		column.setPropertyName( getRelativePath( propertyHolder, propertyName ) );
		// property name + suffix is an "explicit" column name
		if ( isNotEmpty( defaultColumnSuffix ) ) {
			column.setLogicalColumnName( propertyName + defaultColumnSuffix );
			column.setImplicit( false );
		}
		else {
			column.setImplicit( true );
		}
		column.setBuildingContext( context );
		column.bind();
		return column;
	}


	// TODO default name still useful in association table
	public void setJoinAnnotation(JoinColumn joinColumn, String defaultName) {
		if ( joinColumn == null ) {
			setImplicit( true );
		}
		else {
			setImplicit( false );
			if ( !isEmptyAnnotationValue( joinColumn.columnDefinition() ) ) {
				setSqlType( getBuildingContext().getObjectNameNormalizer()
						.applyGlobalQuoting( joinColumn.columnDefinition() ) );
			}
			if ( !isEmptyAnnotationValue( joinColumn.name() ) ) {
				setLogicalColumnName( joinColumn.name() );
			}
			setNullable( joinColumn.nullable() );
			setUnique( joinColumn.unique() );
			setInsertable( joinColumn.insertable() );
			setUpdatable( joinColumn.updatable() );
			setReferencedColumn( joinColumn.referencedColumnName() );

			if ( isEmptyAnnotationValue( joinColumn.table() ) ) {
				setExplicitTableName( "" );
			}
			else {
				final Database database = getBuildingContext().getMetadataCollector().getDatabase();
				final Identifier logicalIdentifier = database.toIdentifier( joinColumn.table() );
				final Identifier physicalIdentifier = getBuildingContext().getBuildingOptions()
						.getPhysicalNamingStrategy()
						.toPhysicalTableName( logicalIdentifier, database.getJdbcEnvironment() );
				setExplicitTableName( physicalIdentifier.render( database.getDialect() ) );
			}
		}
	}

	/**
	 * Build JoinColumn for a JOINED hierarchy
	 */
	public static AnnotatedJoinColumn buildJoinColumn(
			PrimaryKeyJoinColumn primaryKeyJoinColumn,
			JoinColumn joinColumn,
			Value identifier,
			Map<String, Join> joins,
			PropertyHolder propertyHolder,
			MetadataBuildingContext context) {
		final String defaultColumnName = context.getMetadataCollector()
				.getLogicalColumnName( identifier.getTable(),  identifier.getColumns().get(0).getQuotedName() );
		return primaryKeyJoinColumn != null || joinColumn != null
				? explicitJoinColumn( primaryKeyJoinColumn, joinColumn, joins, propertyHolder, context, defaultColumnName )
				: implicitJoinColumn( joins, propertyHolder, context, defaultColumnName );
	}

	private static AnnotatedJoinColumn explicitJoinColumn(
			PrimaryKeyJoinColumn primaryKeyJoinColumn,
			JoinColumn joinColumn,
			Map<String, Join> joins,
			PropertyHolder propertyHolder,
			MetadataBuildingContext context,
			String defaultColumnName) {
		final String columnName;
		final String columnDefinition;
		final String referencedColumnName;
		if ( primaryKeyJoinColumn != null ) {
			columnName = primaryKeyJoinColumn.name();
			columnDefinition = primaryKeyJoinColumn.columnDefinition();
			referencedColumnName = primaryKeyJoinColumn.referencedColumnName();
		}
		else {
			columnName = joinColumn.name();
			columnDefinition = joinColumn.columnDefinition();
			referencedColumnName = joinColumn.referencedColumnName();
		}
		final ObjectNameNormalizer normalizer = context.getObjectNameNormalizer();
		final String columnDef = columnDefinition.isEmpty() ? null
				: normalizer.toDatabaseIdentifierText( columnDefinition );
		final String logicalColumnName = columnName != null && columnName.isEmpty()
				? normalizer.normalizeIdentifierQuotingAsString(defaultColumnName)
				: normalizer.normalizeIdentifierQuotingAsString( columnName );
		final AnnotatedJoinColumn column = new AnnotatedJoinColumn();
		column.setSqlType( columnDef );
		column.setLogicalColumnName( logicalColumnName );
		column.setReferencedColumn( referencedColumnName );
		column.setPropertyHolder(propertyHolder);
		column.setJoins(joins);
		column.setBuildingContext(context);
		column.setImplicit( false );
		column.setNullable( false );
		column.bind();
		return column;
	}

	private static AnnotatedJoinColumn implicitJoinColumn(
			Map<String, Join> joins,
			PropertyHolder propertyHolder,
			MetadataBuildingContext context,
			String defaultColumnName ) {
		final AnnotatedJoinColumn column = new AnnotatedJoinColumn();
		final ObjectNameNormalizer normalizer = context.getObjectNameNormalizer();
		column.setLogicalColumnName( normalizer.normalizeIdentifierQuotingAsString(defaultColumnName) );
		column.setPropertyHolder(propertyHolder);
		column.setJoins(joins);
		column.setBuildingContext(context);
		column.setImplicit( true );
		column.setNullable( false );
		column.bind();
		return column;
	}

	/**
	 * Override persistent class on oneToMany Cases for late settings
	 * Must only be used on second level pass binding
	 */
	public void setPersistentClass(
			PersistentClass persistentClass,
			Map<String, Join> joins,
			Map<XClass, InheritanceState> inheritanceStatePerClass) {
		// TODO shouldn't we deduce the class name from the persistentClass?
		propertyHolder = buildPropertyHolder(
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
		final String logicalReferencedColumn = getBuildingContext().getMetadataCollector()
				.getLogicalColumnName( referencedEntity.getTable(), referencedColumn.getQuotedName() );
		final String columnName = parent.buildDefaultColumnName( referencedEntity, logicalReferencedColumn );
		//yuk side effect on an implicit column
		setLogicalColumnName( columnName );
		setReferencedColumn( logicalReferencedColumn );
		final Column mappingColumn = getMappingColumn();
		initMappingColumn(
				columnName,
				null, referencedColumn.getLength(),
				referencedColumn.getPrecision(),
				referencedColumn.getScale(),
				mappingColumn != null && mappingColumn.isNullable(),
				referencedColumn.getSqlType(),
				mappingColumn != null && mappingColumn.isUnique(),
				false
		);
		linkWithValue( value );
	}

	public void addDefaultJoinColumnName(PersistentClass referencedEntity, String logicalReferencedColumn) {
		final String columnName = parent.buildDefaultColumnName( referencedEntity, logicalReferencedColumn );
		getMappingColumn().setName( columnName );
		setLogicalColumnName( columnName );
	}

	/**
	 * used for mappedBy cases
	 */
	public void linkValueUsingAColumnCopy(Column column, SimpleValue value) {
		initMappingColumn(
				//column.getName(),
				column.getQuotedName(),
				null,
				column.getLength(),
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
		if ( isEmpty( parent.getMappedBy() ) ) {
			// was the column explicitly quoted in the mapping/annotation
			// TODO: in metamodel, we need to better split global quoting and explicit quoting w/ respect to logical names
			boolean isLogicalColumnQuoted = isQuoted( getLogicalColumnName() );
			final ObjectNameNormalizer normalizer = getBuildingContext().getObjectNameNormalizer();
			final String logicalColumnName = normalizer.normalizeIdentifierQuotingAsString( getLogicalColumnName() );
			final String referencedColumn = normalizer.normalizeIdentifierQuotingAsString( getReferencedColumn() );
			final String unquotedLogColName = unquote( logicalColumnName );
			final String unquotedRefColumn = unquote( referencedColumn );
			final String collectionColName = isNotEmpty( unquotedLogColName )
					? unquotedLogColName
					: getPropertyName() + '_' + unquotedRefColumn;
			final InFlightMetadataCollector collector = getBuildingContext().getMetadataCollector();
			final String logicalCollectionColumnName = collector.getDatabase()
					.getJdbcEnvironment()
					.getIdentifierHelper()
					.toIdentifier( collectionColName, isLogicalColumnQuoted )
					.render();
			collector.addColumnNameBinding( value.getTable(), logicalCollectionColumnName, getMappingColumn() );
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
			AnnotatedJoinColumns joinColumns,
			PersistentClass referencedEntity,
			MetadataBuildingContext context) {
		if ( joinColumns.getColumns().length == 0 ) {
			return NO_REFERENCE; //shortcut
		}

		final AnnotatedJoinColumn firstColumn = joinColumns.getColumns()[0];
		final Object columnOwner = findReferencedColumnOwner( referencedEntity, firstColumn, context );
		if ( columnOwner == null ) {
			try {
				throw new MappingException( "A '@JoinColumn' references a column named '"
						+ firstColumn.getReferencedColumn() + "' but the target entity '"
						+ referencedEntity.getEntityName() + "' has no property which maps to this column" );
			}
			catch (MappingException me) {
				// we throw a recoverable exception here in case this
				// is merely an ordering issue, so that the SecondPass
				// will get reprocessed later
				throw new RecoverableException( me.getMessage(), me );
			}
		}
		final Table table = getTable( columnOwner );
		final List<Selectable> keyColumns = referencedEntity.getKey().getSelectables();
		boolean explicitColumnReference = false;
		for ( AnnotatedJoinColumn column : joinColumns.getColumns() ) {
			if ( !column.isReferenceImplicit() ) {
				explicitColumnReference = true;
				if ( !keyColumns.contains( column( context, table, column.getReferencedColumn() ) ) ) {
					// we have a column which does not belong to the PK
					return NON_PK_REFERENCE;
				}
			}
		}
		if ( explicitColumnReference ) {
			// if we got to here, all the columns belong to the PK
			return keyColumns.size() == joinColumns.getColumns().length
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

	private static Table getTable(Object persistentClassOrJoin) {
		return persistentClassOrJoin instanceof PersistentClass
				? ( (PersistentClass) persistentClassOrJoin ).getTable()
				: ( (Join) persistentClassOrJoin ).getTable();
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
		final Column mappingColumn = getMappingColumn();
		if ( mappingColumn != null ) {
			// columnDefinition can also be specified using @JoinColumn, hence we have to check
			// whether it is set or not
			if ( isEmpty( sqlType ) ) {
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

	public static AnnotatedJoinColumns buildJoinTableJoinColumns(
			JoinColumn[] joinColumns,
			Map<String, Join> secondaryTables,
			PropertyHolder propertyHolder,
			String propertyName,
			String mappedBy,
			MetadataBuildingContext context) {
		if ( joinColumns == null ) {
			final AnnotatedJoinColumn column = new AnnotatedJoinColumn();
			column.setImplicit( true );
			column.setNullable( false ); //I break the spec, but it's for good
			column.setPropertyHolder( propertyHolder );
			column.setPropertyName( getRelativePath( propertyHolder, propertyName ) );
			column.setJoins( secondaryTables );
			column.setBuildingContext( context );
			column.bind();
			return AnnotatedJoinColumns.fromColumns( new AnnotatedJoinColumn[] { column }, mappedBy, propertyHolder, context );
		}
		else {
			final AnnotatedJoinColumn[] columns = new AnnotatedJoinColumn[joinColumns.length];
			int length = joinColumns.length;
			for (int index = 0; index < length; index++) {
				final JoinColumn joinColumn = joinColumns[index];
				final AnnotatedJoinColumn column = new AnnotatedJoinColumn();
				column.setImplicit( true );
				column.setPropertyHolder( propertyHolder );
				column.setPropertyName( getRelativePath( propertyHolder, propertyName ) );
				column.setJoins( secondaryTables );
				column.setBuildingContext( context );
				column.setJoinAnnotation( joinColumn, propertyName );
				column.setNullable( false ); //I break the spec, but it's for good
				//done after the annotation to override it
				column.bind();
				columns[index] = column;
			}
			return AnnotatedJoinColumns.fromColumns( columns, mappedBy, propertyHolder, context );
		}
	}

	@Override
	public String toString() {
		StringBuilder string = new StringBuilder();
		string.append( getClass().getSimpleName() ).append( "(" );
		if ( isNotEmpty( getLogicalColumnName() ) ) {
			string.append( "column='" ).append( getLogicalColumnName() ).append( "'," );
		}
		if ( isNotEmpty( referencedColumn ) ) {
			string.append( "referencedColumn='" ).append( referencedColumn ).append( "'," );
		}
		if ( string.charAt( string.length()-1 ) == ',' ) {
			string.setLength( string.length()-1 );
		}
		string.append( ")" );
		return string.toString();
	}

	public void setParent(AnnotatedJoinColumns parent) {
		this.parent = parent;
	}
}

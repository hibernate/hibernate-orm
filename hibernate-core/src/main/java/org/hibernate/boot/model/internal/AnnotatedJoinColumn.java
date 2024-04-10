/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.model.internal;

import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.annotations.JoinFormula;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.ObjectNameNormalizer;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.PropertyData;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Value;

import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrimaryKeyJoinColumn;

import static org.hibernate.boot.model.internal.BinderHelper.getRelativePath;
import static org.hibernate.internal.util.StringHelper.isEmpty;
import static org.hibernate.internal.util.StringHelper.isNotEmpty;
import static org.hibernate.internal.util.StringHelper.isQuoted;
import static org.hibernate.internal.util.StringHelper.nullIfEmpty;
import static org.hibernate.internal.util.StringHelper.qualify;
import static org.hibernate.internal.util.StringHelper.unquote;

/**
 * An element of a join condition, logically representing a
 * {@link jakarta.persistence.JoinColumn} annotation, but not
 * every instance corresponds to an explicit annotation in the
 * Java code.
 * <p>
 * There's no exact analog of this class in the mapping model,
 * so some information is lost when it's transformed into a
 * {@link Column}.
 *
 * @author Emmanuel Bernard
 */
public class AnnotatedJoinColumn extends AnnotatedColumn {

	private String referencedColumn;

	// due to @AnnotationOverride overriding rules,
	// we don't want the constructor to be public
	private AnnotatedJoinColumn() {}

	public void setReferencedColumn(String referencedColumn) {
		this.referencedColumn = nullIfEmpty( referencedColumn );
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
		return isEmpty( referencedColumn );
	}

	static AnnotatedJoinColumn buildJoinColumn(
			JoinColumn joinColumn,
			String mappedBy,
			AnnotatedJoinColumns parent,
			PropertyHolder propertyHolder,
			PropertyData inferredData) {
		final String path = qualify( propertyHolder.getPath(), inferredData.getPropertyName() );
		final JoinColumn[] overrides = propertyHolder.getOverriddenJoinColumn( path );
		if ( overrides != null ) {
			//TODO: relax this restriction
			throw new AnnotationException( "Property '" + path
					+ "' overrides mapping specified using '@JoinColumnOrFormula'" );
		}
		return buildJoinColumn( joinColumn, /*null,*/ mappedBy, parent, propertyHolder, inferredData, "" );
	}

	public static AnnotatedJoinColumn buildJoinFormula(
			JoinFormula joinFormula,
			AnnotatedJoinColumns parent) {
		final AnnotatedJoinColumn formulaColumn = new AnnotatedJoinColumn();
		formulaColumn.setFormula( joinFormula.value() );
		formulaColumn.setReferencedColumn( joinFormula.referencedColumnName() );
//		formulaColumn.setContext( buildingContext );
//		formulaColumn.setPropertyHolder( propertyHolder );
//		formulaColumn.setPropertyName( getRelativePath( propertyHolder, propertyName ) );
//		formulaColumn.setJoins( joins );
		formulaColumn.setParent( parent );
		formulaColumn.bind();
		return formulaColumn;
	}

	static AnnotatedJoinColumn buildJoinColumn(
			JoinColumn joinColumn,
//			Comment comment,
			String mappedBy,
			AnnotatedJoinColumns parent,
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			String defaultColumnSuffix) {
		if ( joinColumn != null ) {
			if ( mappedBy != null ) {
				throw new AnnotationException( "Association '"
						+ getRelativePath( propertyHolder, inferredData.getPropertyName() )
						+ "' is 'mappedBy' a different entity and may not explicitly specify the '@JoinColumn'" );
			}
			return explicitJoinColumn( joinColumn, /*comment,*/ parent, inferredData, defaultColumnSuffix );
		}
		else {
			return implicitJoinColumn( parent, inferredData, defaultColumnSuffix );
		}
	}

	private static AnnotatedJoinColumn explicitJoinColumn(
			JoinColumn joinColumn,
//			Comment comment,
			AnnotatedJoinColumns parent,
			PropertyData inferredData,
			String defaultColumnSuffix) {
		final AnnotatedJoinColumn column = new AnnotatedJoinColumn();
//		column.setComment( comment != null ? comment.value() : null );
//		column.setContext( context );
//		column.setJoins( joins );
//		column.setPropertyHolder( propertyHolder );
		if ( isEmpty( column.getLogicalColumnName() ) && isNotEmpty( defaultColumnSuffix ) ) {
			column.setLogicalColumnName( inferredData.getPropertyName() + defaultColumnSuffix );
		}
//		column.setPropertyName( getRelativePath( propertyHolder, propertyName ) );
		column.setImplicit( false );
		column.setParent( parent );
		column.applyJoinAnnotation( joinColumn, null );
		column.applyColumnDefault( inferredData, parent.getColumns().size() );
		column.bind();
		return column;
	}

	private static AnnotatedJoinColumn implicitJoinColumn(
			AnnotatedJoinColumns parent,
			PropertyData inferredData,
			String defaultColumnSuffix) {
		final AnnotatedJoinColumn column = new AnnotatedJoinColumn();
//		column.setContext( context );
//		column.setJoins( joins );
//		column.setPropertyHolder( propertyHolder );
//		column.setPropertyName( getRelativePath( propertyHolder, propertyName ) );
		// property name + suffix is an "explicit" column name
		if ( isNotEmpty( defaultColumnSuffix ) ) {
			column.setLogicalColumnName( inferredData.getPropertyName() + defaultColumnSuffix );
			column.setImplicit( false );
		}
		else {
			column.setImplicit( true );
		}
		column.setParent( parent );
		column.applyColumnDefault( inferredData, parent.getColumns().size() );
		column.bind();
		return column;
	}


	// TODO default name still useful in association table
	public void applyJoinAnnotation(JoinColumn joinColumn, String defaultName) {
		if ( joinColumn == null ) {
			setImplicit( true );
		}
		else {
			setImplicit( false );
			if ( !joinColumn.columnDefinition().isEmpty() ) {
				setColumnDefinition( joinColumn.columnDefinition() );
				setSqlType( getBuildingContext().getObjectNameNormalizer()
						.applyGlobalQuoting( joinColumn.columnDefinition() ) );
			}
			if ( !joinColumn.name().isEmpty() ) {
				setLogicalColumnName( joinColumn.name() );
			}
			setNullable( joinColumn.nullable() );
			setUnique( joinColumn.unique() );
			setInsertable( joinColumn.insertable() );
			setUpdatable( joinColumn.updatable() );
			setReferencedColumn( joinColumn.referencedColumnName() );

			if ( joinColumn.table().isEmpty() ) {
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
	 * Called for {@link jakarta.persistence.InheritanceType#JOINED} entities.
	 */
	public static AnnotatedJoinColumn buildInheritanceJoinColumn(
			PrimaryKeyJoinColumn primaryKeyJoinColumn,
			JoinColumn joinColumn,
			Value identifier,
			AnnotatedJoinColumns parent,
			MetadataBuildingContext context) {
		final String defaultColumnName = context.getMetadataCollector()
				.getLogicalColumnName( identifier.getTable(), identifier.getColumns().get(0).getQuotedName() );
		return primaryKeyJoinColumn != null || joinColumn != null
				? buildExplicitInheritanceJoinColumn( primaryKeyJoinColumn, joinColumn, parent, context, defaultColumnName )
				: buildImplicitInheritanceJoinColumn( parent, context, defaultColumnName );
	}

	private static AnnotatedJoinColumn buildExplicitInheritanceJoinColumn(
			PrimaryKeyJoinColumn primaryKeyJoinColumn,
			JoinColumn joinColumn,
			AnnotatedJoinColumns parent,
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
		final String logicalColumnName = columnName.isEmpty()
				? normalizer.normalizeIdentifierQuotingAsString( defaultColumnName )
				: normalizer.normalizeIdentifierQuotingAsString( columnName );
		final AnnotatedJoinColumn column = new AnnotatedJoinColumn();
		column.setSqlType( columnDef );
		column.setLogicalColumnName( logicalColumnName );
		column.setReferencedColumn( referencedColumnName );
//		column.setPropertyHolder(propertyHolder);
//		column.setJoins(joins);
//		column.setContext( context );
		column.setImplicit( false );
		column.setNullable( false );
		column.setParent( parent );
		column.bind();
		return column;
	}

	private static AnnotatedJoinColumn buildImplicitInheritanceJoinColumn(
			AnnotatedJoinColumns parent,
			MetadataBuildingContext context,
			String defaultColumnName ) {
		final AnnotatedJoinColumn column = new AnnotatedJoinColumn();
		final ObjectNameNormalizer normalizer = context.getObjectNameNormalizer();
		column.setLogicalColumnName( normalizer.normalizeIdentifierQuotingAsString( defaultColumnName ) );
//		column.setPropertyHolder( propertyHolder );
//		column.setJoins(joins);
//		column.setContext( context );
		column.setImplicit( true );
		column.setNullable( false );
		column.setParent( parent );
		column.bind();
		return column;
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
			linkValueUsingDefaultColumnNaming( synthCol, referencedEntity, value );
		}
		//reset for the future
		setMappingColumn( null );
	}

	public void linkValueUsingDefaultColumnNaming(
			Column referencedColumn,
			PersistentClass referencedEntity,
			SimpleValue value) {
		int columnIndex = getParent().getJoinColumns().indexOf(this);
		linkValueUsingDefaultColumnNaming( columnIndex, referencedColumn, referencedEntity, value );
	}

	public void linkValueUsingDefaultColumnNaming(
			int columnIndex,
			Column referencedColumn,
			PersistentClass referencedEntity,
			SimpleValue value) {
		final String logicalReferencedColumn = getBuildingContext().getMetadataCollector()
				.getLogicalColumnName( referencedEntity.getTable(), referencedColumn.getQuotedName() );
		final String columnName = defaultColumnName( columnIndex, referencedEntity, logicalReferencedColumn );
		//yuk side effect on an implicit column
		setLogicalColumnName( columnName );
		setImplicit( true );
		setReferencedColumn( logicalReferencedColumn );
		final Column mappingColumn = getMappingColumn();
		initMappingColumn(
				columnName,
				null,
				referencedColumn.getLength(),
				referencedColumn.getPrecision(),
				referencedColumn.getScale(),
				referencedColumn.getTemporalPrecision(),
				referencedColumn.getArrayLength(),
				mappingColumn != null && mappingColumn.isNullable(),
				referencedColumn.getSqlType(),
				mappingColumn != null && mappingColumn.isUnique(),
				false
		);
		linkWithValue( value );
	}

	private String defaultColumnName(int columnIndex, PersistentClass referencedEntity, String logicalReferencedColumn) {
		final AnnotatedJoinColumns parent = getParent();
		if ( parent.hasMapsId() ) {
			// infer the join column of the association
			// from the name of the mapped primary key
			// column (this is not required by the JPA
			// spec) and is arguably backwards, given
			// the name of the @MapsId annotation, but
			// it's better than just having two different
			// column names which disagree
			final Column column = parent.resolveMapsId().getValue().getColumns().get( columnIndex );
//			return column.getQuotedName();
			if ( column.isExplicit() ) {
				throw new AnnotationException( "Association '" + parent.getPropertyName()
						+ "' in entity '" + parent.getPropertyHolder().getEntityName()
						+ "' is annotated '@MapsId' but refers to a property '"
						+ parent.getMapsId() + "' which has an explicit column mapping" );
			}
		}
//		else {
			return parent.buildDefaultColumnName( referencedEntity, logicalReferencedColumn );
//		}
	}

	public void addDefaultJoinColumnName(PersistentClass referencedEntity, String logicalReferencedColumn) {
		final String columnName = getParent().buildDefaultColumnName( referencedEntity, logicalReferencedColumn );
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
				column.getTemporalPrecision(),
				column.getArrayLength(),
				getMappingColumn().isNullable(),
				column.getSqlType(),
				getMappingColumn().isUnique(),
				false //We do copy no strategy here
		);
		linkWithValue( value );
	}

	@Override
	protected void addColumnBinding(SimpleValue value) {
		if ( !getParent().hasMappedBy() ) {
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
					: getParent().getPropertyName() + '_' + unquotedRefColumn;
			final InFlightMetadataCollector collector = getBuildingContext().getMetadataCollector();
			final String logicalCollectionColumnName = collector.getDatabase()
					.getJdbcEnvironment()
					.getIdentifierHelper()
					.toIdentifier( collectionColName, isLogicalColumnQuoted )
					.render();
			collector.addColumnNameBinding( value.getTable(), logicalCollectionColumnName, getMappingColumn() );
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
			mappingColumn.setColumnDefinition( column.getColumnDefinition() );
			mappingColumn.setLength( column.getLength() );
			mappingColumn.setPrecision( column.getPrecision() );
			mappingColumn.setScale( column.getScale() );
			mappingColumn.setArrayLength( column.getArrayLength() );
		}
	}

	@Override
	public void redefineColumnName(String columnName, String propertyName, boolean applyNamingStrategy) {
		super.redefineColumnName( columnName, null, applyNamingStrategy );
	}

	static AnnotatedJoinColumn buildImplicitJoinTableJoinColumn(
			AnnotatedJoinColumns parent,
			PropertyHolder propertyHolder,
			PropertyData inferredData) {
		final AnnotatedJoinColumn column = new AnnotatedJoinColumn();
		column.setImplicit( true );

//		column.setPropertyHolder( propertyHolder );
//		column.setPropertyName( getRelativePath( propertyHolder, propertyName ) );
//		column.setJoins( secondaryTables );
//		column.setContext( context );
		column.setParent( parent );
		column.bind();
		column.setNullable( false ); //I break the spec, but it's for good
		return column;
	}

	static AnnotatedJoinColumn buildExplicitJoinTableJoinColumn(
			AnnotatedJoinColumns parent,
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			JoinColumn joinColumn) {
		final AnnotatedJoinColumn column = new AnnotatedJoinColumn();
		column.setImplicit( true );
//		column.setPropertyHolder( propertyHolder );
//		column.setPropertyName( getRelativePath( propertyHolder, propertyName ) );
//		column.setJoins( secondaryTables );
//		column.setContext( context );
		//done after the annotation to override it
		column.setParent( parent );
		column.applyJoinAnnotation( joinColumn, inferredData.getPropertyName() );
		column.bind();
		column.setNullable( false ); //I break the spec, but it's for good
		return column;
	}

	@Override
	public String toString() {
		final StringBuilder string = new StringBuilder();
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

	@Override
	public AnnotatedJoinColumns getParent() {
		return (AnnotatedJoinColumns) super.getParent();
	}

	@Override
	public void setParent(AnnotatedColumns parent) {
		if ( !(parent instanceof  AnnotatedJoinColumns) ) {
			throw new UnsupportedOperationException("wrong kind of parent");
		}
		super.setParent( parent );
	}

	public void setParent(AnnotatedJoinColumns parent) {
		super.setParent( parent );
	}
}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.annotations.JoinFormula;
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.PropertyData;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Value;

import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrimaryKeyJoinColumn;

import java.util.Locale;

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
		return buildJoinColumn( joinColumn, mappedBy, parent, propertyHolder, inferredData, "" );
	}

	public static AnnotatedJoinColumn buildJoinFormula(
			JoinFormula joinFormula,
			AnnotatedJoinColumns parent) {
		final var formulaColumn = new AnnotatedJoinColumn();
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
			String mappedBy,
			AnnotatedJoinColumns parent,
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			String defaultColumnSuffix) {
		if ( joinColumn != null ) {
			if ( mappedBy != null ) {
				throw new AnnotationException(
						String.format(
								Locale.ROOT,
								"Association '%s' of entity '%s' is 'mappedBy' a different entity and may not explicitly specify the '@JoinColumn'",
								inferredData.getPropertyName(),
								propertyHolder.getEntityName() )
				);
			}
			return explicitJoinColumn( joinColumn, parent, inferredData, defaultColumnSuffix );
		}
		else {
			return implicitJoinColumn( parent, inferredData, defaultColumnSuffix );
		}
	}

	private static AnnotatedJoinColumn explicitJoinColumn(
			JoinColumn joinColumn,
			AnnotatedJoinColumns parent,
			PropertyData inferredData,
			String defaultColumnSuffix) {
		final var column = new AnnotatedJoinColumn();
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
		final var column = new AnnotatedJoinColumn();
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

			final var context = getBuildingContext();

			final String name = joinColumn.name();
			if ( !name.isBlank() ) {
				setLogicalColumnName( name );
			}

			final String columnDefinition = joinColumn.columnDefinition();
			if ( !columnDefinition.isBlank() ) {
				setSqlType( context.getObjectNameNormalizer().applyGlobalQuoting( columnDefinition ) );
			}

			setNullable( joinColumn.nullable() );
			setUnique( joinColumn.unique() );
			setInsertable( joinColumn.insertable() );
			setUpdatable( joinColumn.updatable() );
			setReferencedColumn( joinColumn.referencedColumnName() );
			applyColumnCheckConstraint( joinColumn );
			applyColumnComment( joinColumn );
			applyColumnOptions( joinColumn );

			final String table = joinColumn.table();
			if ( table.isBlank() ) {
				setExplicitTableName( "" );
			}
			else {
				final var database = context.getMetadataCollector().getDatabase();
				final var logicalIdentifier = database.toIdentifier( table );
				final var physicalIdentifier =
						context.getBuildingOptions().getPhysicalNamingStrategy()
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
		final String options;
		final String comment;
		if ( primaryKeyJoinColumn != null ) {
			columnName = primaryKeyJoinColumn.name();
			columnDefinition = primaryKeyJoinColumn.columnDefinition();
			referencedColumnName = primaryKeyJoinColumn.referencedColumnName();
			options = primaryKeyJoinColumn.options();
			comment = null;
		}
		else {
			columnName = joinColumn.name();
			columnDefinition = joinColumn.columnDefinition();
			referencedColumnName = joinColumn.referencedColumnName();
			options = joinColumn.options();
			comment = joinColumn.comment();
		}

		final var normalizer = context.getObjectNameNormalizer();
		final String columnDef =
				columnDefinition.isBlank() ? null : normalizer.toDatabaseIdentifierText( columnDefinition );
		final String logicalColumnName =
				normalizer.normalizeIdentifierQuotingAsString( columnName.isBlank() ? defaultColumnName : columnName );
		final var column = new AnnotatedJoinColumn();
		column.setSqlType( columnDef );
		column.setLogicalColumnName( logicalColumnName );
		column.setReferencedColumn( referencedColumnName );
//		column.setPropertyHolder(propertyHolder);
//		column.setJoins(joins);
//		column.setContext( context );
		column.setComment( comment );
		column.setOptions( options );
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
		final var column = new AnnotatedJoinColumn();
		final var normalizer = context.getObjectNameNormalizer();
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

	/**
	 * The JPA-specified rules implemented in
	 * {@link org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl#determineJoinColumnName}
	 * prevent us from assigning defaulted names to {@link JoinColumn} until the second pass.
	 */
	public void linkValueUsingDefaultColumnNaming(
			Column referencedColumn,
			PersistentClass referencedEntity,
			SimpleValue value) {
		final int columnIndex = getParent().getJoinColumns().indexOf( this );
		linkValueUsingDefaultColumnNaming( columnIndex, referencedColumn, referencedEntity, value );
	}

	public void linkValueUsingDefaultColumnNaming(
			int columnIndex,
			Column referencedColumn,
			PersistentClass referencedEntity,
			SimpleValue value) {
		// In the case of a reference to a composite primary key,
		// this instance of AnnotatedJoinColumn actually represents
		// multiple foreign key columns, and this method will be
		// called multiple times on the same instance
		final String logicalReferencedColumn =
				getBuildingContext().getMetadataCollector()
						.getLogicalColumnName( referencedEntity.getTable(), referencedColumn.getQuotedName() );
		final String columnName = defaultColumnName( columnIndex, referencedEntity, logicalReferencedColumn );
		// awful side effect on an implicit column
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
				mappingColumn != null && mappingColumn.getSqlType() != null
						? mappingColumn.getSqlType()
						: referencedColumn.getSqlType(),
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

	/**
	 * Used for {@code mappedBy} cases.
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
			final var context = getBuildingContext();
			// was the column explicitly quoted in the mapping/annotation
			// TODO: in metamodel, we need to better split global quoting and explicit quoting w/ respect to logical names
			boolean isLogicalColumnQuoted = isQuoted( getLogicalColumnName() );
			final var normalizer = context.getObjectNameNormalizer();
			final String logicalColumnName = normalizer.normalizeIdentifierQuotingAsString( getLogicalColumnName() );
			final String referencedColumn = normalizer.normalizeIdentifierQuotingAsString( getReferencedColumn() );
			final String unquotedLogColName = unquote( logicalColumnName );
			final String unquotedRefColumn = unquote( referencedColumn );
			final String collectionColName =
					isNotEmpty( unquotedLogColName )
							? unquotedLogColName
							: getParent().getPropertyName() + '_' + unquotedRefColumn;
			final var collector = context.getMetadataCollector();
			final String logicalCollectionColumnName =
					collector.getDatabase().getJdbcEnvironment().getIdentifierHelper()
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
			mappingColumn.setLength( column.getLength() );
			mappingColumn.setPrecision( column.getPrecision() );
			mappingColumn.setScale( column.getScale() );
			mappingColumn.setArrayLength( column.getArrayLength() );
		}
	}

	/**
	 * Assign the column name from the explicit {@code name} given by the annotation, if any. In cases where the column
	 * name cannot be inferred, the {@link Column} is not assigned a name, and this method returns {@code false}. The
	 * "dummy" {@code Column} will later be replaced with a {@code Column} with name determined by the rules implemented
	 * in {@link org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl#determineJoinColumnName},
	 * as required by the JPA specification, or by any other custom {@link ImplicitNamingStrategy} when
	 * {@link #linkValueUsingDefaultColumnNaming} is called during a {@link org.hibernate.boot.spi.SecondPass}.
	 * @return {@code true} if a name could be inferred
	 */
	@Override
	boolean inferColumnNameIfPossible(String columnName, String propertyName, boolean applyNamingStrategy) {
		if ( isNotEmpty( columnName ) ) {
			getMappingColumn().setName(
					processColumnName( columnName, applyNamingStrategy, isNotEmpty( columnName ) ) );
			return true;
		}
		else {
			return false;
		}
	}

	static AnnotatedJoinColumn buildImplicitJoinTableJoinColumn(
			AnnotatedJoinColumns parent,
			PropertyHolder propertyHolder,
			PropertyData inferredData) {
		final var column = new AnnotatedJoinColumn();
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
		final var column = new AnnotatedJoinColumn();
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
		final var string = new StringBuilder();
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
		if ( !(parent instanceof AnnotatedJoinColumns) ) {
			throw new UnsupportedOperationException("wrong kind of parent");
		}
		super.setParent( parent );
	}

	public void setParent(AnnotatedJoinColumns parent) {
		super.setParent( parent );
	}

	private void applyColumnCheckConstraint(jakarta.persistence.JoinColumn column) {
		applyCheckConstraints( column.check() );
	}

	private void applyColumnOptions(jakarta.persistence.JoinColumn column) {
		options = column.options();
	}

	private void applyColumnComment(jakarta.persistence.JoinColumn column) {
		if ( !column.comment().isBlank() ) {
			comment = column.comment();
		}
	}
}

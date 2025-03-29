/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.AnnotationException;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.Checks;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.FractionalSeconds;
import org.hibernate.annotations.GeneratedColumn;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.ImplicitBasicColumnNameSource;
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.ObjectNameNormalizer;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.source.spi.AttributePath;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.PropertyData;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.AggregateColumn;
import org.hibernate.mapping.CheckConstraint;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Formula;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Table;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.SourceModelBuildingContext;

import static org.hibernate.boot.model.internal.BinderHelper.getPath;
import static org.hibernate.boot.model.internal.BinderHelper.getRelativePath;
import static org.hibernate.boot.model.internal.DialectOverridesAnnotationHelper.getOverridableAnnotation;
import static org.hibernate.internal.CoreLogging.messageLogger;
import static org.hibernate.internal.util.StringHelper.isBlank;
import static org.hibernate.internal.util.StringHelper.isEmpty;
import static org.hibernate.internal.util.StringHelper.isNotEmpty;
import static org.hibernate.internal.util.StringHelper.nullIfBlank;
import static org.hibernate.internal.util.StringHelper.nullIfEmpty;
import static org.hibernate.internal.util.collections.ArrayHelper.isEmpty;
import static org.hibernate.internal.util.collections.CollectionHelper.isNotEmpty;

/**
 * A mapping to a column, logically representing a
 * {@link jakarta.persistence.Column} annotation, but not
 * every instance corresponds to an explicit annotation in
 * the Java code.
 * <p>
 * This class holds a representation that is intermediate
 * between the annotation of the Java source code, and the
 * mapping model object {@link Column}. It's used only by
 * the {@link AnnotationBinder} while parsing annotations,
 * and does not survive into later stages of the startup
 * process.
 *
 * @author Emmanuel Bernard
 */
public class AnnotatedColumn {

	private static final CoreMessageLogger LOG = messageLogger( AnnotatedColumn.class );

	private Column mappingColumn;
	private boolean insertable = true;
	private boolean updatable = true;
	private String explicitTableName; // the JPA @Column annotation lets you specify a table name
	private boolean isImplicit;
	public String sqlType;
	private Long length;
	private Integer precision;
	private Integer scale;
	private Integer temporalPrecision; // technically scale, but most dbs call it precision so...
	private Integer arrayLength;
	private String logicalColumnName;
	private boolean unique;
	private boolean nullable = true;
	private String formulaString;
	private Formula formula;
	private String readExpression;
	private String writeExpression;

	private String defaultValue;
	private String generatedAs;

	private final List<CheckConstraint> checkConstraints = new ArrayList<>();

	private AnnotatedColumns parent;

	String options;
	String comment;

	public AnnotatedColumns getParent() {
		return parent;
	}

	public void setParent(AnnotatedColumns parent) {
		parent.addColumn( this );
		this.parent = parent;
	}

	public String getLogicalColumnName() {
		return logicalColumnName;
	}

	public String getSqlType() {
		return sqlType;
	}

	public Long getLength() {
		return length;
	}

	public Integer getPrecision() {
		return precision;
	}

	public Integer getScale() {
		return scale;
	}

	public Integer getArrayLength() {
		return arrayLength;
	}

	public void setArrayLength(Integer arrayLength) {
		this.arrayLength = arrayLength;
	}

	public boolean isUnique() {
		return unique;
	}

	public boolean isFormula() {
		return isNotEmpty( formulaString );
	}

	public String getExplicitTableName() {
		return explicitTableName;
	}

	public void setExplicitTableName(String explicitTableName) {
		this.explicitTableName = "``".equals( explicitTableName ) ? "" : explicitTableName;
	}

	public void setFormula(String formula) {
		this.formulaString = formula;
	}

	public boolean isImplicit() {
		return isImplicit;
	}

	public void setInsertable(boolean insertable) {
		this.insertable = insertable;
	}

	public void setUpdatable(boolean updatable) {
		this.updatable = updatable;
	}

	public void setImplicit(boolean implicit) {
		isImplicit = implicit;
	}

	public void setSqlType(String sqlType) {
		this.sqlType = sqlType;
	}

	public void setLength(Long length) {
		this.length = length;
	}

	public void setPrecision(Integer precision) {
		this.precision = precision;
	}

	public void setScale(Integer scale) {
		this.scale = scale;
	}

	public void setTemporalPrecision(Integer temporalPrecision) {
		this.temporalPrecision = temporalPrecision;
	}

	public void setLogicalColumnName(String logicalColumnName) {
		this.logicalColumnName = logicalColumnName;
	}

	public void setUnique(boolean unique) {
		this.unique = unique;
	}

	public boolean isNullable() {
		return isFormula() || mappingColumn.isNullable();
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	public void addCheckConstraint(String name, String constraint) {
		checkConstraints.add( new CheckConstraint( name, constraint ) );
	}

	public void addCheckConstraint(String name, String constraint, String options) {
		checkConstraints.add( new CheckConstraint( name, constraint, options ) );
	}

//	public String getComment() {
//		return comment;
//	}

//	public void setComment(String comment) {
//		this.comment = comment;
//	}

	public String getGeneratedAs() {
		return generatedAs;
	}

	private void setGeneratedAs(String as) {
		this.generatedAs = as;
	}

	public AnnotatedColumn() {
	}

	public void bind() {
		if ( isNotEmpty( formulaString ) ) {
			if ( LOG.isDebugEnabled() ) {
				LOG.debug( "Binding formula: " + formulaString );
			}
			formula = new Formula();
			formula.setFormula( formulaString );
		}
		else {
			initMappingColumn(
					logicalColumnName,
					getParent().getPropertyName(),
					length,
					precision,
					scale,
					temporalPrecision,
					arrayLength,
					nullable,
					sqlType,
					unique,
					true
			);
			if ( defaultValue != null ) {
				mappingColumn.setDefaultValue( defaultValue );
			}
			for ( CheckConstraint constraint : checkConstraints ) {
				mappingColumn.addCheckConstraint( constraint );
			}
			mappingColumn.setOptions( options );

			if ( isNotEmpty( comment ) ) {
				mappingColumn.setComment( comment );
			}
			if ( generatedAs != null ) {
				mappingColumn.setGeneratedAs( generatedAs );
			}
			if ( LOG.isDebugEnabled() ) {
				LOG.debug( "Binding column: " + logicalColumnName );
			}
		}
	}

	protected void initMappingColumn(
			String columnName,
			String propertyName,
			Long length,
			Integer precision,
			Integer scale,
			Integer temporalPrecision,
			Integer arrayLength,
			boolean nullable,
			String sqlType,
			boolean unique,
			boolean applyNamingStrategy) {
		if ( isNotEmpty( formulaString ) ) {
			formula = new Formula();
			formula.setFormula( formulaString );
		}
		else {
			mappingColumn = new Column();
			mappingColumn.setExplicit( !isImplicit );
			redefineColumnName( columnName, propertyName, applyNamingStrategy );
			mappingColumn.setLength( length );
			if ( precision != null && precision > 0 ) {  //relevant precision
				mappingColumn.setPrecision( precision );
				mappingColumn.setScale( scale );
			}
			if ( temporalPrecision != null ) {
				mappingColumn.setTemporalPrecision( temporalPrecision );
			}
			mappingColumn.setArrayLength( arrayLength );
			mappingColumn.setNullable( nullable );
			mappingColumn.setSqlType( sqlType );
			if ( unique ) {
				getParent().getTable().createUniqueKey( mappingColumn, getBuildingContext() );
			}
			for ( CheckConstraint constraint : checkConstraints ) {
				mappingColumn.addCheckConstraint( constraint );
			}
			mappingColumn.setDefaultValue( defaultValue );
			mappingColumn.setOptions( options );
			mappingColumn.setComment( comment );

			if ( writeExpression != null ) {
				final int numberOfJdbcParams = StringHelper.count( writeExpression, '?' );
				if ( numberOfJdbcParams != 1 ) {
					throw new AnnotationException(
							"Write expression in '@ColumnTransformer' for property '" + propertyName
							+ "' and column '" + logicalColumnName + "'"
							+ " must contain exactly one placeholder character ('?')"
					);
				}
			}

			mappingColumn.setResolvedCustomRead( readExpression );
			mappingColumn.setCustomWrite( writeExpression );
		}
	}

	public boolean isNameDeferred() {
		return mappingColumn == null || isEmpty( mappingColumn.getName() );
	}

	public void redefineColumnName(String columnName, String propertyName, boolean applyNamingStrategy) {
		if ( StringHelper.isEmpty( columnName ) && StringHelper.isEmpty( propertyName ) ) {
			// nothing to do
			return;
		}
		final String logicalColumnName = resolveLogicalColumnName( columnName, propertyName );
		mappingColumn.setName( processColumnName( logicalColumnName, applyNamingStrategy ) );
	}

	private String resolveLogicalColumnName(String columnName, String propertyName) {
		final String baseColumnName = StringHelper.isNotEmpty( columnName )
				? columnName
				: inferColumnName( propertyName );

		if ( parent.getPropertyHolder() != null && parent.getPropertyHolder().isComponent() ) {
			// see if we need to apply one-or-more @EmbeddedColumnNaming patterns
			return applyEmbeddedColumnNaming( baseColumnName, (ComponentPropertyHolder) parent.getPropertyHolder() );
		}
		else {
			return baseColumnName;
		}
	}

	private String applyEmbeddedColumnNaming(String inferredColumnName, ComponentPropertyHolder propertyHolder) {
		// code
		String result = inferredColumnName;
		boolean appliedAnyPatterns = false;

		final String columnNamingPattern = propertyHolder.getComponent().getColumnNamingPattern();
		if ( StringHelper.isNotEmpty( columnNamingPattern ) ) {
			// zip_code
			result = String.format( columnNamingPattern, result );
			appliedAnyPatterns = true;
		}

		ComponentPropertyHolder tester = propertyHolder;
		while ( tester.parent.isComponent() ) {
			final ComponentPropertyHolder parentHolder = (ComponentPropertyHolder) tester.parent;
			final String parentColumnNamingPattern = parentHolder.getComponent().getColumnNamingPattern();
			if ( StringHelper.isNotEmpty( parentColumnNamingPattern ) ) {
				// 	home_zip_code
				result = String.format( parentColumnNamingPattern, result );
				appliedAnyPatterns = true;
			}
			tester = parentHolder;
		}

		if ( appliedAnyPatterns ) {
			// we need to adjust the logical name to be picked up in `#addColumnBinding`
			this.logicalColumnName = result;
		}

		return result;
	}

	protected String processColumnName(String columnName, boolean applyNamingStrategy) {
		if ( applyNamingStrategy ) {
			final Database database = getBuildingContext().getMetadataCollector().getDatabase();
			return getBuildingContext().getBuildingOptions().getPhysicalNamingStrategy()
					.toPhysicalColumnName( database.toIdentifier( columnName ), database.getJdbcEnvironment() )
					.render( database.getDialect() );
		}
		else {
			return getBuildingContext().getObjectNameNormalizer().toDatabaseIdentifierText( columnName );
		}

	}

	protected String inferColumnName(String propertyName) {
		final Database database = getBuildingContext().getMetadataCollector().getDatabase();
		final ObjectNameNormalizer normalizer = getBuildingContext().getObjectNameNormalizer();
		final ImplicitNamingStrategy implicitNamingStrategy = getBuildingContext().getBuildingOptions().getImplicitNamingStrategy();

		Identifier implicitName = normalizer.normalizeIdentifierQuoting(
				implicitNamingStrategy.determineBasicColumnName(
						new ImplicitBasicColumnNameSource() {
							final AttributePath attributePath = AttributePath.parse( propertyName );

							@Override
							public AttributePath getAttributePath() {
								return attributePath;
							}

							@Override
							public boolean isCollectionElement() {
								// if the propertyHolder is a collection, assume the
								// @Column refers to the element column
								final PropertyHolder propertyHolder = getParent().getPropertyHolder();
								return !propertyHolder.isComponent() && !propertyHolder.isEntity();
							}

							@Override
							public MetadataBuildingContext getBuildingContext() {
								return AnnotatedColumn.this.getBuildingContext();
							}
						}
				)
		);

		// HHH-6005 magic
		if ( implicitName.getText().contains( "_{element}_" ) ) {
			implicitName = Identifier.toIdentifier(
					implicitName.getText().replace( "_{element}_", "_" ),
					implicitName.isQuoted()
			);
		}

		return getBuildingContext().getBuildingOptions().getPhysicalNamingStrategy()
				.toPhysicalColumnName( implicitName, database.getJdbcEnvironment() )
				.render( database.getDialect() );
	}

	public String getName() {
		return mappingColumn.getName();
	}

	public Column getMappingColumn() {
		return mappingColumn;
	}

	public boolean isInsertable() {
		return insertable;
	}

	public boolean isUpdatable() {
		return updatable;
	}

	public void setNullable(boolean nullable) {
		this.nullable = nullable;
		if ( mappingColumn != null ) {
			mappingColumn.setNullable( nullable );
		}
	}

	protected void setMappingColumn(Column mappingColumn) {
		this.mappingColumn = mappingColumn;
	}

	//TODO: move this operation to AnnotatedColumns!!

	public void linkWithAggregateValue(SimpleValue value, Component component) {
		mappingColumn = new AggregateColumn( mappingColumn, component );
		linkWithValue( value );
	}

	public void linkWithValue(SimpleValue value) {
		if ( formula != null ) {
			value.addFormula( formula );
		}
		else {
			final Table table = value.getTable();
			parent.setTable( table );
			mappingColumn.setValue( value );
			value.addColumn( mappingColumn, insertable, updatable );
			table.addColumn( mappingColumn );
			addColumnBinding( value );
		}
	}

	protected void addColumnBinding(SimpleValue value) {
		final String logicalColumnName;
		final MetadataBuildingContext context = getBuildingContext();
		final InFlightMetadataCollector collector = context.getMetadataCollector();
		if ( isNotEmpty( this.logicalColumnName ) ) {
			logicalColumnName = this.logicalColumnName;
		}
		else {
			final Identifier implicitName = context.getObjectNameNormalizer().normalizeIdentifierQuoting(
					context.getBuildingOptions().getImplicitNamingStrategy().determineBasicColumnName(
							new ImplicitBasicColumnNameSource() {
								@Override
								public AttributePath getAttributePath() {
									return AttributePath.parse( getParent().getPropertyName() );
								}

								@Override
								public boolean isCollectionElement() {
									return false;
								}

								@Override
								public MetadataBuildingContext getBuildingContext() {
									return context;
								}
							}
					)
			);
			logicalColumnName = implicitName.render( collector.getDatabase().getDialect() );
		}
		collector.addColumnNameBinding( value.getTable(), logicalColumnName, getMappingColumn() );
	}

	public void forceNotNull() {
		if ( mappingColumn == null ) {
			throw new CannotForceNonNullableException(
					"Cannot perform #forceNotNull because internal org.hibernate.mapping.Column reference is null: " +
							"likely a formula"
			);
		}
		nullable = false;
		mappingColumn.setNullable( false );
	}

	public static AnnotatedColumns buildFormulaFromAnnotation(
			org.hibernate.annotations.Formula formulaAnn,
//			Comment commentAnn,
			Nullability nullability,
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			Map<String, Join> secondaryTables,
			MetadataBuildingContext context) {
		return buildColumnOrFormulaFromAnnotation(
				null,
				formulaAnn,
				null,
//				commentAnn,
				nullability,
				propertyHolder,
				inferredData,
				secondaryTables,
				context
		);
	}

	public static AnnotatedColumns buildColumnFromNoAnnotation(
			FractionalSeconds fractionalSeconds,
//			Comment commentAnn,
			Nullability nullability,
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			Map<String, Join> secondaryTables,
			MetadataBuildingContext context) {
		return buildColumnsFromAnnotations(
				null,
				fractionalSeconds,
//				commentAnn,
				nullability,
				propertyHolder,
				inferredData,
				secondaryTables,
				context
		);
	}

	public static AnnotatedColumns buildColumnFromAnnotation(
			jakarta.persistence.Column column,
			FractionalSeconds fractionalSeconds,
//			Comment commentAnn,
			Nullability nullability,
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			Map<String, Join> secondaryTables,
			MetadataBuildingContext context) {
		return buildColumnOrFormulaFromAnnotation(
				column,
				null,
				fractionalSeconds,
//				commentAnn,
				nullability,
				propertyHolder,
				inferredData,
				secondaryTables,
				context
		);
	}

	public static AnnotatedColumns buildColumnsFromAnnotations(
			jakarta.persistence.Column[] columns,
			FractionalSeconds fractionalSeconds,
//			Comment commentAnn,
			Nullability nullability,
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			Map<String, Join> secondaryTables,
			MetadataBuildingContext context) {
		return buildColumnsOrFormulaFromAnnotation(
				columns,
				null,
				fractionalSeconds,
//				commentAnn,
				nullability,
				propertyHolder,
				inferredData,
				null,
				secondaryTables,
				context
		);
	}

	public static AnnotatedColumns buildColumnsFromAnnotations(
			jakarta.persistence.Column[] columns,
//			Comment commentAnn,
			Nullability nullability,
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			String suffixForDefaultColumnName,
			Map<String, Join> secondaryTables,
			MetadataBuildingContext context) {
		return buildColumnsOrFormulaFromAnnotation(
				columns,
				null,
				null,
//				commentAnn,
				nullability,
				propertyHolder,
				inferredData,
				suffixForDefaultColumnName,
				secondaryTables,
				context
		);
	}

	public static AnnotatedColumns buildColumnOrFormulaFromAnnotation(
			jakarta.persistence.Column column,
			org.hibernate.annotations.Formula formulaAnn,
			FractionalSeconds fractionalSeconds,
//			Comment commentAnn,
			Nullability nullability,
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			Map<String, Join> secondaryTables,
			MetadataBuildingContext context) {
		return buildColumnsOrFormulaFromAnnotation(
				column==null ? null : new jakarta.persistence.Column[] {column},
				formulaAnn,
				fractionalSeconds,
//				commentAnn,
				nullability,
				propertyHolder,
				inferredData,
				null,
				secondaryTables,
				context
		);
	}

	public static AnnotatedColumns buildColumnsOrFormulaFromAnnotation(
			jakarta.persistence.Column[] columns,
			org.hibernate.annotations.Formula formulaAnn,
			FractionalSeconds fractionalSeconds,
//			Comment comment,
			Nullability nullability,
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			String suffixForDefaultColumnName,
			Map<String, Join> secondaryTables,
			MetadataBuildingContext context) {

		if ( formulaAnn != null ) {
			final AnnotatedColumns parent = new AnnotatedColumns();
			parent.setPropertyHolder( propertyHolder );
			parent.setPropertyName( getRelativePath( propertyHolder, inferredData.getPropertyName() ) );
			parent.setBuildingContext( context );
			parent.setJoins( secondaryTables ); //unnecessary
			final AnnotatedColumn formulaColumn = new AnnotatedColumn();
			formulaColumn.setFormula( formulaAnn.value() );
			formulaColumn.setImplicit( false );
//			formulaColumn.setBuildingContext( context );
//			formulaColumn.setPropertyHolder( propertyHolder );
			formulaColumn.setParent( parent );
			formulaColumn.bind();
			return parent;
		}
		else {
			final jakarta.persistence.Column[] actualColumns = overrideColumns( columns, propertyHolder, inferredData );
			if ( isEmpty( actualColumns ) ) {
				return buildImplicitColumn(
						fractionalSeconds,
						inferredData,
						suffixForDefaultColumnName,
						secondaryTables,
						propertyHolder,
//						comment,
						nullability,
						context
				);
			}
			else {
				return buildExplicitColumns(
//						comment,
						propertyHolder,
						inferredData,
						suffixForDefaultColumnName,
						secondaryTables,
						context,
						actualColumns,
						fractionalSeconds
				);
			}
		}
	}

	private static jakarta.persistence.Column[] overrideColumns(
			jakarta.persistence.Column[] columns,
			PropertyHolder propertyHolder,
			PropertyData inferredData ) {
		final String path = getPath( propertyHolder, inferredData );
		final jakarta.persistence.Column[] overriddenCols = propertyHolder.getOverriddenColumn( path );
		if ( overriddenCols != null ) {
			//check for overridden first
			if ( columns != null && overriddenCols.length != columns.length ) {
				//TODO: unfortunately, we never actually see this nice error message, since
				//      PersistentClass.validate() gets called first and produces a worse message
				throw new AnnotationException( "Property '" + path
						+ "' specifies " + columns.length
						+ " '@AttributeOverride's but the overridden property has " + overriddenCols.length
						+ " columns (every column must have exactly one '@AttributeOverride')" );
			}
			if ( LOG.isDebugEnabled() ) {
				LOG.debug( "Column mapping overridden for property: " + inferredData.getPropertyName() );
			}
			return isEmpty( overriddenCols ) ? null : overriddenCols;
		}
		else {
			return columns;
		}
	}

	private static AnnotatedColumns buildExplicitColumns(
//			Comment comment,
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			String suffixForDefaultColumnName,
			Map<String, Join> secondaryTables,
			MetadataBuildingContext context,
			jakarta.persistence.Column[] actualCols,
			FractionalSeconds fractionalSeconds) {
		final AnnotatedColumns parent = new AnnotatedColumns();
		parent.setPropertyHolder( propertyHolder );
		parent.setPropertyName( getRelativePath( propertyHolder, inferredData.getPropertyName() ) );
		parent.setJoins( secondaryTables );
		parent.setBuildingContext( context );
		for ( jakarta.persistence.Column column : actualCols ) {
			final Database database = context.getMetadataCollector().getDatabase();
			final String sqlType = getSqlType( context, column );
			final String tableName = getTableName( column, database );
//						final Identifier logicalName = database.getJdbcEnvironment()
//								.getIdentifierHelper()
//								.toIdentifier( column.table() );
//						final Identifier physicalName = physicalNamingStrategy.toPhysicalTableName( logicalName );
//						tableName = physicalName.render( database.getDialect() );
			buildColumn(
//					comment,
					propertyHolder,
					inferredData,
					suffixForDefaultColumnName,
					parent,
					actualCols.length,
					database,
					column,
					fractionalSeconds,
					sqlType,
					tableName,
					context.getBootstrapContext().getModelsContext()
			);
		}
		return parent;
	}

	private static String getTableName(
			jakarta.persistence.Column column,
			Database database) {
		final String table = column.table();
		return table.isBlank()
				? ""
				: database.getJdbcEnvironment().getIdentifierHelper().toIdentifier( table ).render();
	}

	private static String getSqlType(
			MetadataBuildingContext context,
			jakarta.persistence.Column column) {
		final String columnDefinition = column.columnDefinition();
		return columnDefinition.isBlank()
				? null
				: context.getObjectNameNormalizer().applyGlobalQuoting( columnDefinition );
	}

	private static AnnotatedColumn buildColumn(
//			Comment comment,
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			String suffixForDefaultColumnName,
			AnnotatedColumns parent,
			int numberOfColumns,
			Database database,
			jakarta.persistence.Column column,
			FractionalSeconds fractionalSeconds,
			String sqlType,
			String tableName,
			SourceModelBuildingContext sourceModelContext) {
		final String columnName = logicalColumnName( inferredData, suffixForDefaultColumnName, database, column );
		final AnnotatedColumn annotatedColumn = new AnnotatedColumn();
		annotatedColumn.setLogicalColumnName( columnName );
		annotatedColumn.setImplicit( false );
		annotatedColumn.setSqlType( sqlType );
		annotatedColumn.setLength( (long) column.length() );
		if ( fractionalSeconds != null ) {
			annotatedColumn.setTemporalPrecision( fractionalSeconds.value() );
		}
		else {
			annotatedColumn.setPrecision( column.precision() );
			// The passed annotation could also be a MapKeyColumn
			Integer secondPrecision = column.annotationType() == jakarta.persistence.Column.class
					? column.secondPrecision()
					: null;
			annotatedColumn.setTemporalPrecision( secondPrecision == null || secondPrecision == -1 ? null : secondPrecision );
		}
		annotatedColumn.setScale( column.scale() );
		annotatedColumn.handleArrayLength( inferredData );
		annotatedColumn.setNullable( column.nullable() );
		annotatedColumn.setUnique( column.unique() );
		annotatedColumn.setInsertable( column.insertable() );
		annotatedColumn.setUpdatable( column.updatable() );
		annotatedColumn.setExplicitTableName( tableName );
		annotatedColumn.setParent( parent );
		annotatedColumn.applyColumnDefault( inferredData, numberOfColumns );
		annotatedColumn.applyGeneratedAs( inferredData, numberOfColumns );
		annotatedColumn.applyColumnCheckConstraint( column );
		annotatedColumn.applyColumnOptions( column );
		annotatedColumn.applyColumnComment(column);
		annotatedColumn.applyCheckConstraint( inferredData, numberOfColumns );
		annotatedColumn.extractDataFromPropertyData( propertyHolder, inferredData, sourceModelContext );
		annotatedColumn.bind();
		return annotatedColumn;
	}

	private void handleArrayLength(PropertyData inferredData) {
		final Array arrayAnn = inferredData.getAttributeMember().getDirectAnnotationUsage( Array.class );
		if ( arrayAnn != null ) {
			setArrayLength( arrayAnn.length() );
		}
	}

	private static String logicalColumnName(
			PropertyData inferredData,
			String suffixForDefaultColumnName,
			Database database,
			jakarta.persistence.Column column) {
		final String columnName = getColumnName( database, column );
		// NOTE : this is the logical column name, not the physical!
		return isEmpty( columnName ) && isNotEmpty( suffixForDefaultColumnName )
				? inferredData.getPropertyName() + suffixForDefaultColumnName
				: columnName;
	}

	private static String getColumnName(Database database, jakarta.persistence.Column column) {
		final String name = column.name();
		return name.isBlank()
				? null
				: database.getJdbcEnvironment().getIdentifierHelper().toIdentifier( name ).render();
	}

	void applyColumnDefault(PropertyData inferredData, int length) {
		final MemberDetails attributeMember = inferredData.getAttributeMember();
		if ( attributeMember != null ) {
			final ColumnDefault columnDefault = getOverridableAnnotation(
					attributeMember,
					ColumnDefault.class,
					getBuildingContext()
			);
			if ( columnDefault != null ) {
				if ( length != 1 ) {
					throw new AnnotationException( "'@ColumnDefault' may only be applied to single-column mappings but '"
							+ attributeMember.getName() + "' maps to " + length + " columns" );
				}
				setDefaultValue( columnDefault.value() );
			}
		}
		else {
			LOG.trace("Could not perform @ColumnDefault lookup as 'PropertyData' did not give access to XProperty");
		}
	}

	void applyGeneratedAs(PropertyData inferredData, int length) {
		final MemberDetails attributeMember = inferredData.getAttributeMember();
		if ( attributeMember != null ) {
			final GeneratedColumn generatedColumn = getOverridableAnnotation(
					attributeMember,
					GeneratedColumn.class,
					getBuildingContext()
			);
			if ( generatedColumn != null ) {
				if (length!=1) {
					throw new AnnotationException("'@GeneratedColumn' may only be applied to single-column mappings but '"
							+ attributeMember.getName() + "' maps to " + length + " columns" );
				}
				setGeneratedAs( generatedColumn.value() );
			}
		}
		else {
			LOG.trace("Could not perform @GeneratedColumn lookup as 'PropertyData' did not give access to XProperty");
		}
	}

	private void applyColumnCheckConstraint(jakarta.persistence.Column column) {
		applyCheckConstraints( column.check() );
	}

	void applyCheckConstraints(jakarta.persistence.CheckConstraint[] checkConstraintAnnotationUsages) {
		if ( isNotEmpty( checkConstraintAnnotationUsages ) ) {
			for ( jakarta.persistence.CheckConstraint checkConstraintAnnotationUsage : checkConstraintAnnotationUsages ) {
				addCheckConstraint(
						checkConstraintAnnotationUsage.name(),
						checkConstraintAnnotationUsage.constraint(),
						checkConstraintAnnotationUsage.options()
				);
			}
		}
	}

	void applyCheckConstraint(PropertyData inferredData, int length) {
		final MemberDetails attributeMember = inferredData.getAttributeMember();
		if ( attributeMember != null ) {
			// if there are multiple annotations, they're not overrideable
			final Checks checksAnn = attributeMember.getDirectAnnotationUsage( Checks.class );
			if ( checksAnn != null ) {
				final Check[] checkAnns = checksAnn.value();
				for ( Check checkAnn : checkAnns ) {
					addCheckConstraint( checkAnn.name(), checkAnn.constraints() );
				}
			}
			else {
				final Check checkAnn = getOverridableAnnotation( attributeMember, Check.class, getBuildingContext() );
				if ( checkAnn != null ) {
					if ( length != 1 ) {
						throw new AnnotationException("'@Check' may only be applied to single-column mappings but '"
								+ attributeMember.getName() + "' maps to " + length + " columns (use a table-level '@Check')" );
					}
					addCheckConstraint( nullIfEmpty( checkAnn.name() ), checkAnn.constraints() );
				}
			}
		}
		else {
			LOG.trace("Could not perform @Check lookup as 'PropertyData' did not give access to XProperty");
		}
	}

	//must only be called after all setters are defined and before binding
	private void extractDataFromPropertyData(
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			SourceModelBuildingContext context) {
		if ( inferredData != null ) {
			final MemberDetails attributeMember = inferredData.getAttributeMember();
			if ( attributeMember != null ) {
				if ( propertyHolder.isComponent() ) {
					processColumnTransformerExpressions( propertyHolder.getOverriddenColumnTransformer( logicalColumnName ) );
				}


				attributeMember.forEachAnnotationUsage( ColumnTransformer.class, context, this::processColumnTransformerExpressions );
			}
		}
	}

	private void processColumnTransformerExpressions(ColumnTransformer annotation) {
		if ( annotation == null ) {
			// nothing to process
			return;
		}

		final String targetColumnName = annotation.forColumn();
		if ( isBlank( targetColumnName )
				|| targetColumnName.equals( logicalColumnName != null ? logicalColumnName : "" ) ) {
			readExpression = nullIfBlank( annotation.read() );
			writeExpression = nullIfBlank( annotation.write() );
		}
	}

	private static AnnotatedColumns buildImplicitColumn(
			FractionalSeconds fractionalSeconds,
			PropertyData inferredData,
			String suffixForDefaultColumnName,
			Map<String, Join> secondaryTables,
			PropertyHolder propertyHolder,
//			Comment comment,
			Nullability nullability,
			MetadataBuildingContext context) {
		final AnnotatedColumns columns = new AnnotatedColumns();
		columns.setPropertyHolder( propertyHolder );
		columns.setPropertyName( getRelativePath( propertyHolder, inferredData.getPropertyName() ) );
		columns.setBuildingContext( context );
		columns.setJoins( secondaryTables );
		columns.setPropertyHolder( propertyHolder );
		final AnnotatedColumn column = new AnnotatedColumn();
//		if ( comment != null ) {
//			column.setComment( comment.value() );
//		}
		//not following the spec but more clean
		if ( nullability != Nullability.FORCED_NULL
				&& !PropertyBinder.isOptional( inferredData.getAttributeMember(), propertyHolder ) ) {
			column.setNullable( false );
		}
		final String propertyName = inferredData.getPropertyName();
//		column.setPropertyHolder( propertyHolder );
//		column.setPropertyName( getRelativePath( propertyHolder, propertyName ) );
//		column.setJoins( secondaryTables );
//		column.setBuildingContext( context );
		// property name + suffix is an "explicit" column name
		final boolean implicit = isEmpty( suffixForDefaultColumnName );
		if ( !implicit ) {
			column.setLogicalColumnName( propertyName + suffixForDefaultColumnName );
		}
		column.setImplicit( implicit );
		column.setParent( columns );
		column.applyColumnDefault( inferredData, 1 );
		column.applyGeneratedAs( inferredData, 1 );
		column.applyCheckConstraint( inferredData, 1 );
		column.extractDataFromPropertyData( propertyHolder, inferredData, context.getBootstrapContext().getModelsContext() );
		column.handleArrayLength( inferredData );
		if ( fractionalSeconds != null ) {
			column.setTemporalPrecision( fractionalSeconds.value() );
		}
		column.bind();
		return columns;
	}

	@Override
	public String toString() {
		final StringBuilder string = new StringBuilder();
		string.append( getClass().getSimpleName() ).append( "(" );
		if ( isNotEmpty( formulaString ) ) {
			string.append( "formula='" ).append( formulaString );
		}
		else if ( isNotEmpty( logicalColumnName ) ) {
			string.append( "column='" ).append( logicalColumnName );
		}
		string.append( ")" );
		return string.toString();
	}

	MetadataBuildingContext getBuildingContext() {
		return getParent().getBuildingContext();
	}

	private void applyColumnOptions(jakarta.persistence.Column column) {
		options = column.options();
	}

	private void applyColumnComment(jakarta.persistence.Column column) {
		if ( !column.comment().isBlank() ) {
			comment = column.comment();
		}
	}

	void setOptions(String options){
		this.options = options;
	}

	void setComment(String comment){
		this.comment = comment;
	}
}

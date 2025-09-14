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
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.source.spi.AttributePath;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.PropertyData;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.AggregateColumn;
import org.hibernate.mapping.CheckConstraint;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Formula;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Table;
import org.hibernate.models.spi.ModelsContext;

import static org.hibernate.boot.model.internal.BinderHelper.getPath;
import static org.hibernate.boot.model.internal.BinderHelper.getRelativePath;
import static org.hibernate.boot.model.internal.DialectOverridesAnnotationHelper.getOverridableAnnotation;
import static org.hibernate.internal.CoreMessageLogger.CORE_LOGGER;
import static org.hibernate.internal.util.StringHelper.isBlank;
import static org.hibernate.internal.util.StringHelper.isEmpty;
import static org.hibernate.internal.util.StringHelper.isNotEmpty;
import static org.hibernate.internal.util.StringHelper.nullIfBlank;
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
			if ( CORE_LOGGER.isTraceEnabled() ) {
				CORE_LOGGER.trace( "Binding formula: " + formulaString );
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
			if ( CORE_LOGGER.isDebugEnabled() && logicalColumnName != null ) {
				CORE_LOGGER.trace( "Binding column: " + logicalColumnName );
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
			final boolean nameDetermined =
					inferColumnNameIfPossible( columnName, propertyName, applyNamingStrategy );
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
			mappingColumn.setUnique( unique );
			// if the column name is not determined, we will assign the
			// name to the unique key later this method gets called again
			// from linkValueUsingDefaultColumnNaming() in second pass
			if ( unique && nameDetermined ) {
				// assign a unique key name to the column
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

	/**
	 * Attempt to infer the column name from the explicit {@code name} given by the annotation and the property or field
	 * name. In the case of a {@link jakarta.persistence.JoinColumn}, this is impossible, due to the rules implemented in
	 * {@link org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl#determineJoinColumnName}. In cases
	 * where the column name cannot be inferred, the {@link Column} is not assigned a name, and this method returns
	 * {@code false}. The "dummy" {@code Column} will later be replaced with a {@code Column} with a name determined by
	 * the {@link ImplicitNamingStrategy} when {@link AnnotatedJoinColumn#linkValueUsingDefaultColumnNaming} is called
	 * during a {@link org.hibernate.boot.spi.SecondPass}.
	 * @return {@code true} if a name could be inferred
	 */
	boolean inferColumnNameIfPossible(String columnName, String propertyName, boolean applyNamingStrategy) {
		if ( !isEmpty( columnName ) || !isEmpty( propertyName ) ) {
			final String logicalColumnName = resolveLogicalColumnName( columnName, propertyName );
			mappingColumn.setName( processColumnName( logicalColumnName, applyNamingStrategy ) );
			return true;
		}
		else {
			return false;
		}
	}

	private String resolveLogicalColumnName(String columnName, String propertyName) {
		final String baseColumnName = isNotEmpty( columnName ) ? columnName : inferColumnName( propertyName );
		return parent.getPropertyHolder() != null && parent.getPropertyHolder().isComponent()
				// see if we need to apply one-or-more @EmbeddedColumnNaming patterns
				? applyEmbeddedColumnNaming( baseColumnName, (ComponentPropertyHolder) parent.getPropertyHolder() )
				: baseColumnName;
	}

	private String applyEmbeddedColumnNaming(String inferredColumnName, ComponentPropertyHolder propertyHolder) {
		// code
		String result = inferredColumnName;
		boolean appliedAnyPatterns = false;

		final String columnNamingPattern = propertyHolder.getComponent().getColumnNamingPattern();
		if ( isNotEmpty( columnNamingPattern ) ) {
			// zip_code
			result = String.format( columnNamingPattern, result );
			appliedAnyPatterns = true;
		}

		ComponentPropertyHolder tester = propertyHolder;
		while ( tester.parent.isComponent() ) {
			final var parentHolder = (ComponentPropertyHolder) tester.parent;
			final String parentColumnNamingPattern = parentHolder.getComponent().getColumnNamingPattern();
			if ( isNotEmpty( parentColumnNamingPattern ) ) {
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
			final var database = getDatabase();
			return getPhysicalNamingStrategy()
					.toPhysicalColumnName( database.toIdentifier( columnName ), database.getJdbcEnvironment() )
					.render( database.getDialect() );
		}
		else {
			return getObjectNameNormalizer().toDatabaseIdentifierText( columnName );
		}
	}

	protected String inferColumnName(String propertyName) {
		Identifier implicitName = getObjectNameNormalizer().normalizeIdentifierQuoting(
				getImplicitNamingStrategy().determineBasicColumnName(
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
								final var propertyHolder = getParent().getPropertyHolder();
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

		return implicitName.render( getDatabase().getDialect() );
	}

	private ObjectNameNormalizer getObjectNameNormalizer() {
		return getBuildingContext().getObjectNameNormalizer();
	}

	private Database getDatabase() {
		return getBuildingContext().getMetadataCollector().getDatabase();
	}

	private PhysicalNamingStrategy getPhysicalNamingStrategy() {
		return getBuildingContext().getBuildingOptions().getPhysicalNamingStrategy();
	}

	private ImplicitNamingStrategy getImplicitNamingStrategy() {
		return getBuildingContext().getBuildingOptions().getImplicitNamingStrategy();
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
		if ( isNotEmpty( this.logicalColumnName ) ) {
			logicalColumnName = this.logicalColumnName;
		}
		else {
			final Identifier implicitName = getObjectNameNormalizer().normalizeIdentifierQuoting(
					getImplicitNamingStrategy().determineBasicColumnName(
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
									return AnnotatedColumn.this.getBuildingContext();
								}
							}
					)
			);
			logicalColumnName = implicitName.render( getDatabase().getDialect() );
		}
		getBuildingContext().getMetadataCollector()
				.addColumnNameBinding( value.getTable(), logicalColumnName, getMappingColumn() );
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

	public static AnnotatedColumns buildColumnFromAnnotations(
			jakarta.persistence.Column column,
//			Comment commentAnn,
			Nullability nullability,
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			String suffixForDefaultColumnName,
			Map<String, Join> secondaryTables,
			MetadataBuildingContext context) {
		return buildColumnsOrFormulaFromAnnotation(
				column == null
						? null
						: new jakarta.persistence.Column[] {column},
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
				column==null
						? null
						: new jakarta.persistence.Column[] {column},
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
			final var parent = new AnnotatedColumns();
			parent.setPropertyHolder( propertyHolder );
			parent.setPropertyName( getRelativePath( propertyHolder, inferredData.getPropertyName() ) );
			parent.setBuildingContext( context );
			parent.setJoins( secondaryTables ); //unnecessary
			final var formulaColumn = new AnnotatedColumn();
			formulaColumn.setFormula( formulaAnn.value() );
			formulaColumn.setImplicit( false );
//			formulaColumn.setBuildingContext( context );
//			formulaColumn.setPropertyHolder( propertyHolder );
			formulaColumn.setParent( parent );
			formulaColumn.bind();
			return parent;
		}
		else {
			final var actualColumns = overrideColumns( columns, propertyHolder, inferredData );
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
		final var overriddenCols = propertyHolder.getOverriddenColumn( path );
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
			if ( CORE_LOGGER.isTraceEnabled() ) {
				CORE_LOGGER.trace( "Column mapping overridden for property: " + inferredData.getPropertyName() );
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
		final var parent = new AnnotatedColumns();
		parent.setPropertyHolder( propertyHolder );
		parent.setPropertyName( getRelativePath( propertyHolder, inferredData.getPropertyName() ) );
		parent.setJoins( secondaryTables );
		parent.setBuildingContext( context );
		final var database = context.getMetadataCollector().getDatabase();
		for ( var column : actualCols ) {
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
					getSqlType( context, column ),
					getTableName( column, database ),
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
			ModelsContext sourceModelContext) {
		final String columnName = logicalColumnName( inferredData, suffixForDefaultColumnName, database, column );
		final var annotatedColumn = new AnnotatedColumn();
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
			annotatedColumn.setTemporalPrecision( temporalPrecision( column ) );
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

	private static Integer temporalPrecision(jakarta.persistence.Column column) {
		final Integer secondPrecision =
				column.annotationType() == jakarta.persistence.Column.class
						? column.secondPrecision()
						: null;
		return secondPrecision == null || secondPrecision == -1
				? null
				: secondPrecision;
	}

	private void handleArrayLength(PropertyData inferredData) {
		final var arrayAnn = inferredData.getAttributeMember().getDirectAnnotationUsage( Array.class );
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
		// NOTE: this is the logical column name, not the physical!
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
		final var memberDetails = inferredData.getAttributeMember();
		if ( memberDetails != null ) {
			final var columnDefault = getOverridableAnnotation(
					memberDetails,
					ColumnDefault.class,
					getBuildingContext()
			);
			if ( columnDefault != null ) {
				if ( length != 1 ) {
					throw new AnnotationException( "'@ColumnDefault' may only be applied to single-column mappings but '"
							+ memberDetails.getName() + "' maps to " + length + " columns" );
				}
				setDefaultValue( columnDefault.value() );
			}
		}
		else {
			CORE_LOGGER.trace("Could not perform @ColumnDefault lookup as 'PropertyData' did not give access to XProperty");
		}
	}

	void applyGeneratedAs(PropertyData inferredData, int length) {
		final var memberDetails = inferredData.getAttributeMember();
		if ( memberDetails != null ) {
			final var generatedColumn = getOverridableAnnotation(
					memberDetails,
					GeneratedColumn.class,
					getBuildingContext()
			);
			if ( generatedColumn != null ) {
				if (length!=1) {
					throw new AnnotationException("'@GeneratedColumn' may only be applied to single-column mappings but '"
							+ memberDetails.getName() + "' maps to " + length + " columns" );
				}
				setGeneratedAs( generatedColumn.value() );
			}
		}
		else {
			CORE_LOGGER.trace("Could not perform @GeneratedColumn lookup as 'PropertyData' did not give access to XProperty");
		}
	}

	private void applyColumnCheckConstraint(jakarta.persistence.Column column) {
		applyCheckConstraints( column.check() );
	}

	void applyCheckConstraints(jakarta.persistence.CheckConstraint[] checkConstraintAnnotationUsages) {
		if ( isNotEmpty( checkConstraintAnnotationUsages ) ) {
			for ( var checkConstraintAnnotationUsage : checkConstraintAnnotationUsages ) {
				addCheckConstraint(
						nullIfBlank( checkConstraintAnnotationUsage.name() ),
						checkConstraintAnnotationUsage.constraint(),
						checkConstraintAnnotationUsage.options()
				);
			}
		}
	}

	void applyCheckConstraint(PropertyData inferredData, int length) {
		final var memberDetails = inferredData.getAttributeMember();
		if ( memberDetails != null ) {
			// if there are multiple annotations, they're not overrideable
			final var checksAnn = memberDetails.getDirectAnnotationUsage( Checks.class );
			if ( checksAnn != null ) {
				final var checkAnns = checksAnn.value();
				for ( var checkAnn : checkAnns ) {
					addCheckConstraint( nullIfBlank( checkAnn.name() ), checkAnn.constraints() );
				}
			}
			else {
				final var checkAnn = getOverridableAnnotation( memberDetails, Check.class, getBuildingContext() );
				if ( checkAnn != null ) {
					if ( length != 1 ) {
						throw new AnnotationException("'@Check' may only be applied to single-column mappings but '"
								+ memberDetails.getName() + "' maps to " + length + " columns (use a table-level '@Check')" );
					}
					addCheckConstraint( nullIfBlank( checkAnn.name() ), checkAnn.constraints() );
				}
			}
		}
		else {
			CORE_LOGGER.trace("Could not perform @Check lookup as 'PropertyData' did not give access to XProperty");
		}
	}

	//must only be called after all setters are defined and before binding
	private void extractDataFromPropertyData(
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			ModelsContext context) {
		if ( inferredData != null ) {
			final var memberDetails = inferredData.getAttributeMember();
			if ( memberDetails != null ) {
				if ( propertyHolder.isComponent() ) {
					processColumnTransformerExpressions( propertyHolder.getOverriddenColumnTransformer( logicalColumnName ) );
				}
				memberDetails.forEachAnnotationUsage( ColumnTransformer.class, context, this::processColumnTransformerExpressions );
			}
		}
	}

	private void processColumnTransformerExpressions(ColumnTransformer annotation) {
		if ( annotation != null ) {
			final String targetColumnName = annotation.forColumn();
			if ( isBlank( targetColumnName )
					|| targetColumnName.equals( logicalColumnName != null ? logicalColumnName : "" ) ) {
				readExpression = nullIfBlank( annotation.read() );
				writeExpression = nullIfBlank( annotation.write() );
			}
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
		final var columns = new AnnotatedColumns();
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
		final var string = new StringBuilder();
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

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg;

import java.util.Map;

import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.MappingException;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.GeneratedColumn;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.ColumnTransformers;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.ImplicitBasicColumnNameSource;
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.ObjectNameNormalizer;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.source.spi.AttributePath;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.cfg.annotations.Nullability;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Formula;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Table;

import org.jboss.logging.Logger;

import static org.hibernate.cfg.BinderHelper.getOverridableAnnotation;
import static org.hibernate.cfg.BinderHelper.getPath;
import static org.hibernate.cfg.BinderHelper.getRelativePath;
import static org.hibernate.cfg.BinderHelper.isEmptyAnnotationValue;
import static org.hibernate.internal.util.StringHelper.isEmpty;
import static org.hibernate.internal.util.StringHelper.isNotEmpty;
import static org.hibernate.internal.util.StringHelper.nullIfEmpty;
import static org.hibernate.internal.util.StringHelper.qualify;

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

	private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, AnnotatedColumn.class.getName());

	private MetadataBuildingContext context;

	private Column mappingColumn;
	private boolean insertable = true;
	private boolean updatable = true;
	private String explicitTableName; // the JPA @Column annotation lets you specify a table name
	protected Map<String, Join> joins;
	@Deprecated // use AnnotatedColumns.propertyHolder
	protected PropertyHolder propertyHolder;
	private boolean isImplicit;
	public String sqlType;
	private Long length;
	private Integer precision;
	private Integer scale;
	private String logicalColumnName;
	private String propertyName;  // this is really a .-separated property path
	private boolean unique;
	private boolean nullable = true;
	private String formulaString;
	private Formula formula;
	private String readExpression;
	private String writeExpression;

	private String defaultValue;
	private String generatedAs;

	private String comment;
	private String checkConstraint;

	private AnnotatedColumns parent;

	void setParent(AnnotatedColumns parent) {
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

	public boolean isUnique() {
		return unique;
	}

	public boolean isFormula() {
		return isNotEmpty( formulaString );
	}

	public String getFormulaString() {
		return formulaString;
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

	protected MetadataBuildingContext getBuildingContext() {
		return context;
	}

	public void setBuildingContext(MetadataBuildingContext context) {
		this.context = context;
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

	public void setLogicalColumnName(String logicalColumnName) {
		this.logicalColumnName = logicalColumnName;
	}

	public void setPropertyName(String propertyName) {
		this.propertyName = propertyName;
	}

	/**
	 * A property path relative to the {@link #getPropertyHolder() PropertyHolder}.
	 */
	public String getPropertyName() {
		return propertyName;
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

	public String getCheckConstraint() {
		return checkConstraint;
	}

	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	public void setCheckConstraint(String checkConstraint) {
		this.checkConstraint = checkConstraint;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

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
			LOG.debugf( "Binding formula %s", formulaString );
			formula = new Formula();
			formula.setFormula( formulaString );
		}
		else {
			initMappingColumn(
					logicalColumnName,
					propertyName,
					length,
					precision,
					scale,
					nullable,
					sqlType,
					unique,
					true
			);
			if ( defaultValue != null ) {
				mappingColumn.setDefaultValue( defaultValue );
			}
			if ( checkConstraint !=null ) {
				mappingColumn.setCheckConstraint( checkConstraint );
			}
			if ( isNotEmpty( comment ) ) {
				mappingColumn.setComment( comment );
			}
			if ( generatedAs != null ) {
				mappingColumn.setGeneratedAs( generatedAs );
			}
			if ( LOG.isDebugEnabled() ) {
				LOG.debugf( "Binding column: %s", toString() );
			}
		}
	}

	protected void initMappingColumn(
			String columnName,
			String propertyName,
			Long length,
			Integer precision,
			Integer scale,
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
			redefineColumnName( columnName, propertyName, applyNamingStrategy );
			mappingColumn.setLength( length );
			if ( precision != null && precision > 0 ) {  //relevant precision
				mappingColumn.setPrecision( precision );
				mappingColumn.setScale( scale );
			}
			mappingColumn.setNullable( nullable );
			mappingColumn.setSqlType( sqlType );
			mappingColumn.setUnique( unique );
			mappingColumn.setCheckConstraint( checkConstraint );

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
		if ( isNotEmpty( columnName ) ) {
			mappingColumn.setName( processColumnName( columnName, applyNamingStrategy ) );
		}
		else {
			if ( propertyName != null && applyNamingStrategy ) {
				mappingColumn.setName( inferColumnName( propertyName ) );
			}
			//Do nothing otherwise
		}
	}

	private String processColumnName(String columnName, boolean applyNamingStrategy) {
		if ( applyNamingStrategy ) {
			final Database database = context.getMetadataCollector().getDatabase();
			return context.getBuildingOptions().getPhysicalNamingStrategy()
					.toPhysicalColumnName( database.toIdentifier( columnName ), database.getJdbcEnvironment() )
					.render( database.getDialect() );
		}
		else {
			return context.getObjectNameNormalizer().toDatabaseIdentifierText( columnName );
		}

	}

	private String inferColumnName(String propertyName) {
		final Database database = context.getMetadataCollector().getDatabase();
		final ObjectNameNormalizer normalizer = context.getObjectNameNormalizer();
		final ImplicitNamingStrategy implicitNamingStrategy = context.getBuildingOptions().getImplicitNamingStrategy();

		Identifier implicitName = normalizer.normalizeIdentifierQuoting(
				implicitNamingStrategy.determineBasicColumnName(
						new ImplicitBasicColumnNameSource() {
							final AttributePath attributePath = AttributePath.parse(propertyName);

							@Override
							public AttributePath getAttributePath() {
								return attributePath;
							}

							@Override
							public boolean isCollectionElement() {
								// if the propertyHolder is a collection, assume the
								// @Column refers to the element column
								return !getPropertyHolder().isComponent()
									&& !getPropertyHolder().isEntity();
							}

							@Override
							public MetadataBuildingContext getBuildingContext() {
								return context;
							}
						}
				)
		);

		// HHH-6005 magic
		if ( implicitName.getText().contains( "_collection&&element_" ) ) {
			implicitName = Identifier.toIdentifier(
					implicitName.getText().replace( "_collection&&element_", "_" ),
					implicitName.isQuoted()
			);
		}

		return context.getBuildingOptions().getPhysicalNamingStrategy()
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
		if ( mappingColumn != null ) {
			mappingColumn.setNullable( nullable );
		}
		else {
			this.nullable = nullable;
		}
	}

	public void setJoins(Map<String, Join> joins) {
		this.joins = joins;
	}

	public PropertyHolder getPropertyHolder() {
		return propertyHolder; //TODO: change this to delegate to the parent
	}

	@Deprecated // use AnnotatedColumns.setPropertyHolder() instead
	public void setPropertyHolder(PropertyHolder propertyHolder) {
		this.propertyHolder = propertyHolder;
	}

	protected void setMappingColumn(Column mappingColumn) {
		this.mappingColumn = mappingColumn;
	}

	//TODO: move this operation to AnnotatedColumns!!
	public void linkWithValue(SimpleValue value) {
		if ( formula != null ) {
			value.addFormula( formula );
		}
		else {
			final Table table = value.getTable();
			if ( parent != null ) {
				parent.setTableInternal( table );
			}
			getMappingColumn().setValue( value );
			value.addColumn( getMappingColumn(), insertable, updatable );
			table.addColumn( getMappingColumn() );
			addColumnBinding( value );
		}
	}

	protected void addColumnBinding(SimpleValue value) {
		final String logicalColumnName;
		if ( isNotEmpty( this.logicalColumnName ) ) {
			logicalColumnName = this.logicalColumnName;
		}
		else {
			final ObjectNameNormalizer normalizer = context.getObjectNameNormalizer();
			final Database database = context.getMetadataCollector().getDatabase();
			final ImplicitNamingStrategy implicitNamingStrategy = context.getBuildingOptions()
					.getImplicitNamingStrategy();

			final Identifier implicitName = normalizer.normalizeIdentifierQuoting(
					implicitNamingStrategy.determineBasicColumnName(
							new ImplicitBasicColumnNameSource() {
								@Override
								public AttributePath getAttributePath() {
									return AttributePath.parse( propertyName );
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
			logicalColumnName = implicitName.render( database.getDialect() );
		}
		context.getMetadataCollector().addColumnNameBinding( value.getTable(), logicalColumnName, getMappingColumn() );
	}

	/**
	 * Find appropriate table of the column.
	 * It can come from a secondary table or from the main table of the persistent class
	 *
	 * @return appropriate table
	 * @throws AnnotationException missing secondary table
	 */
	public Table getTable() {
		return parent.getTable();
	}

	//TODO: move to AnnotatedColumns
	public boolean isSecondary() {
		if ( propertyHolder == null ) {
			throw new AssertionFailure( "Should not call isSecondary() on column w/o persistent class defined" );
		}
		return isNotEmpty( explicitTableName )
			&& !getPropertyHolder().getTable().getName().equals( explicitTableName );
	}

	//TODO: move to AnnotatedColumns
	public Join getJoin() {
		Join join = joins.get( explicitTableName );
		if ( join == null ) {
			// annotation binding seems to use logical and physical naming somewhat inconsistently...
			final String physicalTableName = getBuildingContext().getMetadataCollector()
					.getPhysicalTableName( explicitTableName );
			if ( physicalTableName != null ) {
				join = joins.get( physicalTableName );
			}
		}

		if ( join == null ) {
			throw new AnnotationException(
					"Secondary table '" + explicitTableName + "' for property '" + getPropertyHolder().getClassName()
					+ "' is not declared (use '@SecondaryTable' to declare the secondary table)"
			);
		}

		return join;
	}

	public void forceNotNull() {
		if ( mappingColumn == null ) {
			throw new CannotForceNonNullableException(
					"Cannot perform #forceNotNull because internal org.hibernate.mapping.Column reference is null: " +
							"likely a formula"
			);
		}
		mappingColumn.setNullable( false );
	}

	public static AnnotatedColumns buildFormulaFromAnnotation(
			org.hibernate.annotations.Formula formulaAnn,
			Comment commentAnn,
			Nullability nullability,
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			Map<String, Join> secondaryTables,
			MetadataBuildingContext context) {
		return buildColumnOrFormulaFromAnnotation(
				null,
				formulaAnn,
				commentAnn,
				nullability,
				propertyHolder,
				inferredData,
				secondaryTables,
				context
		);
	}

	public static AnnotatedColumns buildColumnFromNoAnnotation(
			Comment commentAnn,
			Nullability nullability,
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			Map<String, Join> secondaryTables,
			MetadataBuildingContext context) {
		return buildColumnsFromAnnotations(
				null,
				commentAnn,
				nullability,
				propertyHolder,
				inferredData,
				secondaryTables,
				context
		);
	}

	public static AnnotatedColumns buildColumnFromAnnotation(
			jakarta.persistence.Column column,
			Comment commentAnn,
			Nullability nullability,
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			Map<String, Join> secondaryTables,
			MetadataBuildingContext context) {
		return buildColumnOrFormulaFromAnnotation(
				column,
				null,
				commentAnn,
				nullability,
				propertyHolder,
				inferredData,
				secondaryTables,
				context
		);
	}

	public static AnnotatedColumns buildColumnsFromAnnotations(
			jakarta.persistence.Column[] columns,
			Comment commentAnn,
			Nullability nullability,
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			Map<String, Join> secondaryTables,
			MetadataBuildingContext context) {
		return buildColumnsOrFormulaFromAnnotation(
				columns,
				null,
				commentAnn,
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
			Comment commentAnn,
			Nullability nullability,
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			String suffixForDefaultColumnName,
			Map<String, Join> secondaryTables,
			MetadataBuildingContext context) {
		return buildColumnsOrFormulaFromAnnotation(
				columns,
				null,
				commentAnn,
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
			Comment commentAnn,
			Nullability nullability,
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			Map<String, Join> secondaryTables,
			MetadataBuildingContext context) {
		return buildColumnsOrFormulaFromAnnotation(
				new jakarta.persistence.Column[] { column },
				formulaAnn,
				commentAnn,
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
			Comment comment,
			Nullability nullability,
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			String suffixForDefaultColumnName,
			Map<String, Join> secondaryTables,
			MetadataBuildingContext context) {

		if ( formulaAnn != null ) {
			AnnotatedColumn formulaColumn = new AnnotatedColumn();
			formulaColumn.setFormula( formulaAnn.value() );
			formulaColumn.setImplicit( false );
			formulaColumn.setBuildingContext( context );
			formulaColumn.setPropertyHolder( propertyHolder );
			formulaColumn.bind();
			final AnnotatedColumns result = new AnnotatedColumns();
			result.setPropertyHolder( propertyHolder );
			result.setColumns( new AnnotatedColumn[] {formulaColumn} );
			return result;
		}
		else {
			final jakarta.persistence.Column[]  actualColumns = overrideColumns( columns, propertyHolder, inferredData );
			if ( actualColumns == null ) {
				return buildImplicitColumn(
						inferredData,
						suffixForDefaultColumnName,
						secondaryTables,
						propertyHolder,
						comment,
						nullability,
						context
				);
			}
			else {
				return buildExplicitColumns(
						comment,
						propertyHolder,
						inferredData,
						suffixForDefaultColumnName,
						secondaryTables,
						context,
						actualColumns
				);
			}
		}
	}

	private static jakarta.persistence.Column[] overrideColumns(
			jakarta.persistence.Column[] columns,
			PropertyHolder propertyHolder,
			PropertyData inferredData ) {
		final jakarta.persistence.Column[] overriddenCols = propertyHolder.getOverriddenColumn(
				qualify( propertyHolder.getPath(), inferredData.getPropertyName() )
		);
		if ( overriddenCols != null ) {
			//check for overridden first
			if ( columns != null && overriddenCols.length != columns.length ) {
				//TODO: unfortunately, we never actually see this nice error message, since
				//      PersistentClass.validate() gets called first and produces a worse message
				throw new AnnotationException( "Property '" + getPath( propertyHolder, inferredData )
						+ "' specifies " + columns.length
						+ " '@AttributeOverride's but the overridden property has " + overriddenCols.length
						+ " columns (every column must have exactly one '@AttributeOverride')" );
			}
			LOG.debugf( "Column(s) overridden for property %s", inferredData.getPropertyName() );
			return overriddenCols.length == 0 ? null : overriddenCols;
		}
		else {
			return columns;
		}
	}

	private static AnnotatedColumns buildExplicitColumns(
			Comment comment,
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			String suffixForDefaultColumnName,
			Map<String, Join> secondaryTables,
			MetadataBuildingContext context,
			jakarta.persistence.Column[] actualCols) {
		final int length = actualCols.length;
		final AnnotatedColumn[] columns = new AnnotatedColumn[length];
		for ( int index = 0; index < length; index++ ) {
			final jakarta.persistence.Column column = actualCols[index];
			final Database database = context.getMetadataCollector().getDatabase();
			final String sqlType = getSqlType( context, column );
			final String tableName = getTableName( column, database );
//						final Identifier logicalName = database.getJdbcEnvironment()
//								.getIdentifierHelper()
//								.toIdentifier( column.table() );
//						final Identifier physicalName = physicalNamingStrategy.toPhysicalTableName( logicalName );
//						tableName = physicalName.render( database.getDialect() );
			columns[index] = buildColumn(
					comment,
					propertyHolder,
					inferredData,
					suffixForDefaultColumnName,
					secondaryTables,
					context,
					length,
					database,
					column,
					sqlType,
					tableName
			);
		}
		final AnnotatedColumns result = new AnnotatedColumns();
		result.setPropertyHolder(propertyHolder);
		result.setColumns(columns);
		return result;
	}

	private static String getTableName(jakarta.persistence.Column column, Database database) {
		return isEmptyAnnotationValue( column.table() ) ? ""
				: database.getJdbcEnvironment().getIdentifierHelper().toIdentifier( column.table() ).render();
	}

	private static String getSqlType(MetadataBuildingContext context, jakarta.persistence.Column column) {
		return isEmptyAnnotationValue( column.columnDefinition() ) ? null
				: context.getObjectNameNormalizer().applyGlobalQuoting( column.columnDefinition() );
	}

	private static AnnotatedColumn buildColumn(
			Comment comment,
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			String suffixForDefaultColumnName,
			Map<String, Join> secondaryTables,
			MetadataBuildingContext context,
			int length,
			Database database,
			jakarta.persistence.Column col,
			String sqlType,
			String tableName) {

		final AnnotatedColumn column = new AnnotatedColumn();
		column.setLogicalColumnName( getLogicalColumnName( inferredData, suffixForDefaultColumnName, database, col ) );
		column.setImplicit( false );
		column.setSqlType(sqlType);
		column.setLength( (long) col.length() );
		column.setPrecision( col.precision() );
		column.setScale( col.scale() );
		column.setPropertyHolder( propertyHolder );
		column.setPropertyName( getRelativePath( propertyHolder, inferredData.getPropertyName() ) );
		column.setNullable( col.nullable() ); //TODO force to not null if available? This is a (bad) user choice.
		if ( comment != null ) {
			column.setComment( comment.value() );
		}
		column.setUnique( col.unique() );
		column.setInsertable( col.insertable() );
		column.setUpdatable( col.updatable() );
		column.setExplicitTableName( tableName );
		column.setJoins( secondaryTables );
		column.setBuildingContext( context );
		column.applyColumnDefault( inferredData, length );
		column.applyGeneratedAs( inferredData, length );
		column.applyCheckConstraint( inferredData, length );
		column.extractDataFromPropertyData( inferredData );
		column.bind();
		return column;
	}

	private static String getLogicalColumnName(
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
		return isEmptyAnnotationValue( column.name() ) ? null
				: database.getJdbcEnvironment().getIdentifierHelper().toIdentifier( column.name() ).render();
	}

	private void applyColumnDefault(PropertyData inferredData, int length) {
		final XProperty xProperty = inferredData.getProperty();
		if ( xProperty != null ) {
			final ColumnDefault columnDefault = getOverridableAnnotation( xProperty, ColumnDefault.class, context );
			if ( columnDefault != null ) {
				if ( length!=1 ) {
					throw new MappingException("@ColumnDefault may only be applied to single-column mappings");
				}
				setDefaultValue( columnDefault.value() );
			}
		}
		else {
			LOG.trace("Could not perform @ColumnDefault lookup as 'PropertyData' did not give access to XProperty");
		}
	}

	private void applyGeneratedAs(PropertyData inferredData, int length) {
		final XProperty xProperty = inferredData.getProperty();
		if ( xProperty != null ) {
			final GeneratedColumn generatedColumn = getOverridableAnnotation( xProperty, GeneratedColumn.class, context );
			if ( generatedColumn != null ) {
				if (length!=1) {
					throw new MappingException("@GeneratedColumn may only be applied to single-column mappings");
				}
				setGeneratedAs( generatedColumn.value() );
			}
		}
		else {
			LOG.trace("Could not perform @GeneratedColumn lookup as 'PropertyData' did not give access to XProperty");
		}
	}

	private void applyCheckConstraint(PropertyData inferredData, int length) {
		final XProperty xProperty = inferredData.getProperty();
		if ( xProperty != null ) {
			final Check check = getOverridableAnnotation( xProperty, Check.class, context );
			if ( check != null ) {
				if (length!=1) {
					throw new MappingException("@Check may only be applied to single-column mappings (use a table-level @Check)");
				}
				setCheckConstraint( check.constraints() );
			}
		}
		else {
			LOG.trace("Could not perform @Check lookup as 'PropertyData' did not give access to XProperty");
		}
	}

	//must only be called after all setters are defined and before binding
	private void extractDataFromPropertyData(PropertyData inferredData) {
		if ( inferredData != null ) {
			XProperty property = inferredData.getProperty();
			if ( property != null ) {
				if ( getPropertyHolder().isComponent() ) {
					processColumnTransformerExpressions( getPropertyHolder().getOverriddenColumnTransformer( logicalColumnName ) );
				}
				processColumnTransformerExpressions( property.getAnnotation( ColumnTransformer.class ) );
				final ColumnTransformers annotations = property.getAnnotation( ColumnTransformers.class );
				if ( annotations != null ) {
					for ( ColumnTransformer annotation : annotations.value() ) {
						processColumnTransformerExpressions( annotation );
					}
				}
			}
		}
	}

	private void processColumnTransformerExpressions(ColumnTransformer annotation) {
		if ( annotation != null ) {
			if ( isEmpty( annotation.forColumn() )
					// "" is the default value for annotations
					|| annotation.forColumn().equals( logicalColumnName != null ? logicalColumnName : "" ) ) {
				readExpression = nullIfEmpty( annotation.read() );
				writeExpression = nullIfEmpty( annotation.write() );
			}
		}
	}

	private static AnnotatedColumns buildImplicitColumn(
			PropertyData inferredData,
			String suffixForDefaultColumnName,
			Map<String, Join> secondaryTables,
			PropertyHolder propertyHolder,
			Comment comment,
			Nullability nullability,
			MetadataBuildingContext context) {
		final AnnotatedColumns result = new AnnotatedColumns();
		result.setPropertyHolder( propertyHolder );
		final AnnotatedColumn column = bindImplicitColumn(
				inferredData,
				suffixForDefaultColumnName,
				secondaryTables,
				propertyHolder,
				comment,
				nullability,
				context
		);
		result.setColumns( new AnnotatedColumn[] { column } );
		return result;
	}

	private static AnnotatedColumn bindImplicitColumn(
			PropertyData inferredData,
			String suffixForDefaultColumnName,
			Map<String, Join> secondaryTables,
			PropertyHolder propertyHolder,
			Comment comment,
			Nullability nullability,
			MetadataBuildingContext context) {
		final AnnotatedColumn column = new AnnotatedColumn();
		if ( comment != null ) {
			column.setComment( comment.value() );
		}
		//not following the spec but more clean
		if ( nullability != Nullability.FORCED_NULL
				&& inferredData.getClassOrElement().isPrimitive()
				&& !inferredData.getProperty().isArray() ) {
			column.setNullable( false );
		}
		final String propertyName = inferredData.getPropertyName();
		column.setPropertyHolder( propertyHolder );
		column.setPropertyName( getRelativePath( propertyHolder, propertyName ) );
		column.setJoins( secondaryTables );
		column.setBuildingContext( context );
		// property name + suffix is an "explicit" column name
		boolean implicit = isEmpty( suffixForDefaultColumnName );
		if ( !implicit ) {
			column.setLogicalColumnName( propertyName + suffixForDefaultColumnName );
		}
		column.setImplicit( implicit );
		column.applyColumnDefault( inferredData, 1 );
		column.applyGeneratedAs( inferredData, 1 );
		column.applyCheckConstraint( inferredData, 1 );
		column.extractDataFromPropertyData( inferredData );
		column.bind();
		return column;
	}

	public static void checkPropertyConsistency(AnnotatedColumn[] columns, String propertyName) {
		if ( columns.length > 1 ) {
			for (int currentIndex = 1; currentIndex < columns.length; currentIndex++) {
				if ( !columns[currentIndex].isFormula() && !columns[currentIndex - 1].isFormula() ) {
					if ( columns[currentIndex].isNullable() != columns[currentIndex - 1].isNullable() ) {
						throw new AnnotationException(
								"Column mappings for property '" + propertyName + "' mix nullable with 'not null'"
						);
					}
					if ( columns[currentIndex].isInsertable() != columns[currentIndex - 1].isInsertable() ) {
						throw new AnnotationException(
								"Column mappings for property '" + propertyName + "' mix insertable with 'insertable=false'"
						);
					}
					if ( columns[currentIndex].isUpdatable() != columns[currentIndex - 1].isUpdatable() ) {
						throw new AnnotationException(
								"Column mappings for property '" + propertyName + "' mix updatable with 'updatable=false'"
						);
					}
					if ( !columns[currentIndex].getTable().equals( columns[currentIndex - 1].getTable() ) ) {
						throw new AnnotationException(
								"Column mappings for property '" + propertyName + "' mix distinct secondary tables"
						);
					}
				}
			}
		}
	}

	public void addIndex(Index index, boolean inSecondPass) {
		if ( index != null ) {
			addIndex( index.name(), inSecondPass );
		}
	}

	void addIndex(String indexName, boolean inSecondPass) {
		IndexOrUniqueKeySecondPass secondPass = new IndexOrUniqueKeySecondPass( indexName, this, context, false );
		if ( inSecondPass ) {
			secondPass.doSecondPass( context.getMetadataCollector().getEntityBindingMap() );
		}
		else {
			context.getMetadataCollector().addSecondPass( secondPass );
		}
	}

	void addUniqueKey(String uniqueKeyName, boolean inSecondPass) {
		IndexOrUniqueKeySecondPass secondPass = new IndexOrUniqueKeySecondPass( uniqueKeyName, this, context, true );
		if ( inSecondPass ) {
			secondPass.doSecondPass( context.getMetadataCollector().getEntityBindingMap() );
		}
		else {
			context.getMetadataCollector().addSecondPass( secondPass );
		}
	}

	@Override
	public String toString() {
		StringBuilder string = new StringBuilder();
		string.append( getClass().getSimpleName() ).append( "(" );
		if ( isNotEmpty( logicalColumnName ) ) {
			string.append( "column='" ).append( logicalColumnName ).append( "'," );
		}
		if ( isNotEmpty( formulaString ) ) {
			string.append( "formula='" ).append( formulaString ).append( "'," );
		}
		if ( string.charAt( string.length()-1 ) == ',' ) {
			string.setLength( string.length()-1 );
		}
		string.append( ")" );
		return string.toString();
	}
}

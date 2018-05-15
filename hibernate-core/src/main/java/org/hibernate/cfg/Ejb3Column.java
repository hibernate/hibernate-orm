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
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.ColumnTransformers;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.boot.model.naming.ImplicitBasicColumnNameSource;
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.ObjectNameNormalizer;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.MappedTable;
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
import org.hibernate.naming.Identifier;

import org.jboss.logging.Logger;

/**
 * Wrap state of an EJB3 @Column annotation
 * and build the Hibernate column mapping element
 *
 * @author Emmanuel Bernard
 */
public class Ejb3Column {

	private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, Ejb3Column.class.getName());

	private MetadataBuildingContext context;

	private Column mappingColumn;
	private boolean insertable = true;
	private boolean updatable = true;
	private String explicitTableName;
	protected Map<String, Join> joins;
	protected PropertyHolder propertyHolder;
	private boolean isImplicit;
	public String sqlType;
	private Long length;
	private Integer precision;
	private Integer scale;
	private Identifier logicalColumnName;
	private String propertyName;
	private boolean unique;
	private boolean nullable = true;
	private String formulaString;
	private Formula formula;
	private MappedTable table;
	private String readExpression;
	private String writeExpression;

	private String defaultValue;


	/**
	 * @deprecated since 6.0, use {@link #setTable(MappedTable)} instead.
	 */
	@Deprecated
	public void setTable(Table table) {
		this.table = table;
	}

	public void setTable(MappedTable table) {
		this.table = table;
	}

	public Identifier getLogicalColumnName() {
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
		return StringHelper.isNotEmpty( formulaString );
	}

	@SuppressWarnings("UnusedDeclaration")
	public String getFormulaString() {
		return formulaString;
	}

	@SuppressWarnings("UnusedDeclaration")
	public String getExplicitTableName() {
		return explicitTableName;
	}

	public void setExplicitTableName(String explicitTableName) {
		if ( "``".equals( explicitTableName ) ) {
			this.explicitTableName = "";
		}
		else {
			this.explicitTableName = explicitTableName;
		}
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

	public void setLogicalColumnName(Identifier logicalColumnName) {
		this.logicalColumnName = logicalColumnName;
	}

	public void setLogicalColumnName(String logicalColumnName) {
		this.logicalColumnName = buildLogicalName( logicalColumnName );
	}

	public void setPropertyName(String propertyName) {
		this.propertyName = propertyName;
	}

	public String getPropertyName() {
		return propertyName;
	}

	public void setUnique(boolean unique) {
		this.unique = unique;
	}

	public boolean isNullable() {
		return isFormula() ? true : mappingColumn.isNullable();
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	public Ejb3Column(MetadataBuildingContext buildingContext) {
		this.context = buildingContext;
	}

	public void bind() {
		if ( StringHelper.isNotEmpty( formulaString ) ) {
			LOG.debugf( "Binding formula %s", formulaString );
			formula = new Formula(formulaString);
		}
		else {
			initMappingColumn(
					logicalColumnName, propertyName, length, precision, scale, nullable, sqlType, unique, true
			);
			if ( defaultValue != null ) {
				mappingColumn.setDefaultValue( defaultValue );
			}
			if ( LOG.isDebugEnabled() ) {
				LOG.debugf( "Binding column: %s", toString() );
			}
		}
	}

	protected void initMappingColumn(
			Identifier columnName,
			String propertyName,
			Long length,
			Integer precision,
			Integer scale,
			boolean nullable,
			String sqlType,
			boolean unique,
			boolean applyNamingStrategy) {
		if ( StringHelper.isNotEmpty( formulaString ) ) {
			this.formula = new Formula(formulaString);
		}
		else {
			this.mappingColumn = new Column(redefineColumnName( columnName, propertyName, applyNamingStrategy ), unique);
			;
			this.mappingColumn.setLength( length );
			if ( precision != null && precision > 0 ) {  //revelent precision
				this.mappingColumn.setPrecision( precision );
				this.mappingColumn.setScale( scale );
			}
			this.mappingColumn.setNullable( nullable );
			this.mappingColumn.setSqlType( sqlType );

			if(writeExpression != null && !writeExpression.matches("[^?]*\\?[^?]*")) {
				throw new AnnotationException(
						"@WriteExpression must contain exactly one value placeholder ('?') character: property ["
								+ propertyName + "] and column [" + logicalColumnName + "]"
				);
			}
			if ( readExpression != null) {
				this.mappingColumn.setCustomRead( readExpression );
			}
			if ( writeExpression != null) {
				this.mappingColumn.setCustomWrite( writeExpression );
			}
		}
	}

	public boolean isNameDeferred() {
		return mappingColumn == null || StringHelper.isEmpty( mappingColumn.getText() );
	}

	public Identifier redefineColumnName(Identifier columnName, String propertyName, boolean applyNamingStrategy) {
		final ObjectNameNormalizer normalizer = context.getObjectNameNormalizer();
		final ImplicitNamingStrategy implicitNamingStrategy = context.getBuildingOptions().getImplicitNamingStrategy();

		if ( applyNamingStrategy ) {
			if ( columnName == null ) {
				if ( propertyName != null ) {
					final AttributePath attributePath = AttributePath.parse( propertyName );

					Identifier implicitName = normalizer.normalizeIdentifierQuoting(
							implicitNamingStrategy.determineBasicColumnName(
									new ImplicitBasicColumnNameSource() {
										@Override
										public AttributePath getAttributePath() {
											return attributePath;
										}

										@Override
										public boolean isCollectionElement() {
											// if the propertyHolder is a collection, assume the
											// @Column refers to the element column
											return !propertyHolder.isComponent()
													&& !propertyHolder.isEntity();
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
						implicitName = Identifier.toIdentifier( implicitName.getText().replace( "_collection&&element_", "_" ),
								implicitName.isQuoted() );
					}

					return implicitName;
				}
				//Do nothing otherwise
			}
			else {
				return columnName;
			}
		}
		else {
			if ( columnName != null) {
				return normalizer.normalizeIdentifierQuoting( columnName );
			}
		}
		return null;
	}

	public String getName() {
		return mappingColumn.getText();
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
		return propertyHolder;
	}

	public void setPropertyHolder(PropertyHolder propertyHolder) {
		this.propertyHolder = propertyHolder;
	}

	protected void setMappingColumn(Column mappingColumn) {
		this.mappingColumn = mappingColumn;
	}

	public void linkWithValue(SimpleValue value) {
		if ( formula != null ) {
			value.addFormula( formula );
		}
		else {
			table = value.getMappedTable();
			final Column mappingColumn = getMappingColumn();
			if ( table != null ) {
				mappingColumn.setTableName( table.getNameIdentifier() );
			}
			table.addColumn( mappingColumn );
			value.addColumn( mappingColumn );
		}
	}

	/**
	 * Find appropriate table of the column.
	 * It can come from a secondary table or from the main table of the persistent class
	 *
	 * @return appropriate table
	 * @throws AnnotationException missing secondary table
	 *
	 * @deprecated since 6.0, use {@link #getMappedTable()} instead
	 */
	public Table getTable() {
		return (Table) getMappedTable();
	}

	public MappedTable getMappedTable() {
		if ( table != null ){
			return table;
		}

		if ( isSecondary() ) {
			return getJoin().getMappedTable();
		}
		else {
			return propertyHolder.getTable();
		}
	}

	public boolean isSecondary() {
		if ( propertyHolder == null ) {
			throw new AssertionFailure( "Should not call getTable() on column w/o persistent class defined" );
		}

		return StringHelper.isNotEmpty( explicitTableName )
				&& !propertyHolder.getTable().getName().equals( explicitTableName );
	}

	public Join getJoin() {
		Join join = joins.get( explicitTableName );
		if ( join == null ) {
			// annotation binding seems to use logical and physical naming somewhat inconsistently...
			if ( explicitTableName != null ) {
				join = joins.get( explicitTableName );
			}
		}

		if ( join == null ) {
			throw new AnnotationException(
					"Cannot find the expected secondary table: no "
							+ explicitTableName + " available for " + propertyHolder.getClassName()
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

	public static Ejb3Column[] buildColumnFromAnnotation(
			javax.persistence.Column[] anns,
			org.hibernate.annotations.Formula formulaAnn,
			Nullability nullability,
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			Map<String, Join> secondaryTables,
			MetadataBuildingContext context) {
		return buildColumnFromAnnotation(
				anns,
				formulaAnn,
				nullability,
				propertyHolder,
				inferredData,
				null,
				secondaryTables,
				context
		);
	}

	public static Ejb3Column[] buildColumnFromAnnotation(
			javax.persistence.Column[] anns,
			org.hibernate.annotations.Formula formulaAnn,
			Nullability nullability,
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			String suffixForDefaultColumnName,
			Map<String, Join> secondaryTables,
			MetadataBuildingContext context) {
		Ejb3Column[] columns;
		if ( formulaAnn != null ) {
			Ejb3Column formulaColumn = new Ejb3Column( context );
			formulaColumn.setFormula( formulaAnn.value() );
			formulaColumn.setImplicit( false );
			formulaColumn.setPropertyHolder( propertyHolder );
			formulaColumn.bind();
			columns = new Ejb3Column[] { formulaColumn };
		}
		else {
			javax.persistence.Column[] actualCols = anns;
			javax.persistence.Column[] overriddenCols = propertyHolder.getOverriddenColumn(
					StringHelper.qualify( propertyHolder.getPath(), inferredData.getPropertyName() )
			);
			if ( overriddenCols != null ) {
				//check for overridden first
				if ( anns != null && overriddenCols.length != anns.length ) {
					throw new AnnotationException( "AttributeOverride.column() should override all columns for now" );
				}
				actualCols = overriddenCols.length == 0 ? null : overriddenCols;
				LOG.debugf( "Column(s) overridden for property %s", inferredData.getPropertyName() );
			}
			if ( actualCols == null ) {
				columns = buildImplicitColumn(
						inferredData,
						suffixForDefaultColumnName,
						secondaryTables,
						propertyHolder,
						nullability,
						context
				);
			}
			else {
				final int length = actualCols.length;
				columns = new Ejb3Column[length];
				for (int index = 0; index < length; index++) {

					final ObjectNameNormalizer normalizer = context.getObjectNameNormalizer();
					final Database database = context.getMetadataCollector().getDatabase();

					javax.persistence.Column col = actualCols[index];

					final String sqlType;
					if ( col.columnDefinition().equals( "" ) ) {
						sqlType = null;
					}
					else {
						sqlType = normalizer.applyGlobalQuoting( col.columnDefinition() );
					}

					final String tableName;
					if ( StringHelper.isEmpty( col.table() ) ) {
						tableName = "";
					}
					else {
						tableName = database.getJdbcEnvironment()
								.getIdentifierHelper()
								.toIdentifier( col.table() )
								.render();
//						final Identifier logicalName = database.getJdbcEnvironment()
//								.getIdentifierHelper()
//								.toIdentifier( col.table() );
//						final Identifier physicalName = physicalNamingStrategy.toPhysicalTableName( logicalName );
//						tableName = physicalName.render( database.getDialect() );
					}

					final Identifier columnName;
					if ( "".equals( col.name() ) ) {
						columnName = null;
					}
					else {
						// NOTE : this is the logical column name, not the physical!
						columnName = buildLogicalName( database, col.name() );
					}

					Ejb3Column column = new Ejb3Column( context );

					if ( length == 1 ) {
						applyColumnDefault( column, inferredData );
					}

					column.setImplicit( false );
					column.setSqlType( sqlType );
					column.setLength( interpretLength( col ) );
					column.setPrecision( interpretPrecision( col ) );
					column.setScale( col.scale() );
					if ( columnName == null && !StringHelper.isEmpty( suffixForDefaultColumnName ) ) {
						column.setLogicalColumnName( buildLogicalName( database, inferredData.getPropertyName() + suffixForDefaultColumnName ) );
					}
					else {
						column.setLogicalColumnName( columnName );
					}

					column.setPropertyName(
							BinderHelper.getRelativePath( propertyHolder, inferredData.getPropertyName() )
					);
					column.setNullable(
						col.nullable()
					); //TODO force to not null if available? This is a (bad) user choice.
					column.setUnique( col.unique() );
					column.setInsertable( col.insertable() );
					column.setUpdatable( col.updatable() );
					column.setExplicitTableName( tableName );
					column.setPropertyHolder( propertyHolder );
					column.setJoins( secondaryTables );
					column.extractDataFromPropertyData(inferredData);
					column.bind();
					columns[index] = column;
				}
			}
		}
		return columns;
	}

	private static Long interpretLength(javax.persistence.Column col) {
		if ( col == null ) {
			return null;
		}

		if ( col.length() == 255 ) {
			// JPA annotation defines 255 as the default value for this
			//		Column#length attribute.  Until we us Jandex we
			//		do not know whether the 255 here was explicitly set
			//		or is just the JVM-applied default.  We assume the later
			//		which creates a small possibility of an "error" for that
			//		specific value if explicitly set
			return null;
		}

		return (long) col.length();
	}

	private static Integer interpretPrecision(javax.persistence.Column col) {
		if ( col == null ) {
			return null;
		}

		if ( col.precision() == 0 ) {
			// JPA annotation defines 0 as the default value for this
			//		Column#precision attribute.  Until we us Jandex we
			//		do not know whether the 0 here was explicitly set
			//		or is just the JVM-applied default.  We assume the later
			//		which creates a small possibility of an "error" for that
			//		specific value if explicitly set
			return null;
		}

		return col.precision();
	}

	private static Integer interpretScale(javax.persistence.Column col) {
		if ( col == null ) {
			return null;
		}

		if ( col.scale() == 0 ) {
			// JPA annotation defines 0 as the default value for this
			//		Column#scale attribute.  Until we us Jandex we
			//		do not know whether the 0 here was explicitly set
			//		or is just the JVM-applied default.  We assume the later
			//		which creates a small possibility of an "error" for that
			//		specific value if explicitly set
			return null;
		}

		return col.scale();
	}

	public static Identifier buildLogicalName(Database database, String name) {
		return database.getJdbcEnvironment()
				.getIdentifierHelper()
				.toIdentifier( name );
	}

	private Identifier buildLogicalName(String name) {
		return buildLogicalName( context.getMetadataCollector().getDatabase(), name );
	}

	private static void applyColumnDefault(Ejb3Column column, PropertyData inferredData) {
		final XProperty xProperty = inferredData.getProperty();
		if ( xProperty != null ) {
			ColumnDefault columnDefaultAnn = xProperty.getAnnotation( ColumnDefault.class );
			if ( columnDefaultAnn != null ) {
				column.setDefaultValue( columnDefaultAnn.value() );
			}
		}
		else {
			LOG.trace(
					"Could not perform @ColumnDefault lookup as 'PropertyData' did not give access to XProperty"
			);
		}
	}

	//must only be called after all setters are defined and before bind
	private void extractDataFromPropertyData(PropertyData inferredData) {
		if ( inferredData != null ) {
			XProperty property = inferredData.getProperty();
			if ( property != null ) {
				processExpression( property.getAnnotation( ColumnTransformer.class ) );
				ColumnTransformers annotations = property.getAnnotation( ColumnTransformers.class );
				if (annotations != null) {
					for ( ColumnTransformer annotation : annotations.value() ) {
						processExpression( annotation );
					}
				}
			}
		}
	}

	private void processExpression(ColumnTransformer annotation) {
		if ( annotation == null ) {
			return;
		}

		Identifier nonNullLogicalColumnName =
				logicalColumnName != null ? logicalColumnName : Identifier.toIdentifier( "" );

		if ( StringHelper.isEmpty( annotation.forColumn() )
				|| annotation.forColumn().equals( nonNullLogicalColumnName ) ) {
			readExpression = annotation.read();
			if ( StringHelper.isEmpty( readExpression ) ) {
				readExpression = null;
			}
			writeExpression = annotation.write();
			if ( StringHelper.isEmpty( writeExpression ) ) {
				writeExpression = null;
			}
		}
	}

	private static Ejb3Column[] buildImplicitColumn(
			PropertyData inferredData,
			String suffixForDefaultColumnName,
			Map<String, Join> secondaryTables,
			PropertyHolder propertyHolder,
			Nullability nullability,
			MetadataBuildingContext context) {
		Ejb3Column column = new Ejb3Column( context );
		Ejb3Column[] columns = new Ejb3Column[1];
		columns[0] = column;

		//not following the spec but more clean
		if ( nullability != Nullability.FORCED_NULL
				&& inferredData.getClassOrElement().isPrimitive()
				&& !inferredData.getProperty().isArray() ) {
			column.setNullable( false );
		}
		final String propertyName = inferredData.getPropertyName();
		column.setPropertyName(
				BinderHelper.getRelativePath( propertyHolder, propertyName )
		);
		column.setPropertyHolder( propertyHolder );
		column.setJoins( secondaryTables );

		// property name + suffix is an "explicit" column name
		if ( !StringHelper.isEmpty( suffixForDefaultColumnName ) ) {
			column.setLogicalColumnName(
					buildLogicalName(
							context.getMetadataCollector().getDatabase(),
							propertyName + suffixForDefaultColumnName
					)
			);
			column.setImplicit( false );
		}
		else {
			column.setImplicit( true );
		}

		applyColumnDefault( column, inferredData );
		column.extractDataFromPropertyData( inferredData );
		column.bind();
		return columns;
	}

	public static void checkPropertyConsistency(Ejb3Column[] columns, String propertyName) {
		int nbrOfColumns = columns.length;

		if ( nbrOfColumns > 1 ) {
			for (int currentIndex = 1; currentIndex < nbrOfColumns; currentIndex++) {

				if (columns[currentIndex].isFormula() || columns[currentIndex - 1].isFormula()) {
					continue;
				}

				if ( columns[currentIndex].isInsertable() != columns[currentIndex - 1].isInsertable() ) {
					throw new AnnotationException(
							"Mixing insertable and non insertable columns in a property is not allowed: " + propertyName
					);
				}
				if ( columns[currentIndex].isNullable() != columns[currentIndex - 1].isNullable() ) {
					throw new AnnotationException(
							"Mixing nullable and non nullable columns in a property is not allowed: " + propertyName
					);
				}
				if ( columns[currentIndex].isUpdatable() != columns[currentIndex - 1].isUpdatable() ) {
					throw new AnnotationException(
							"Mixing updatable and non updatable columns in a property is not allowed: " + propertyName
					);
				}
				if ( !columns[currentIndex].getTable().equals( columns[currentIndex - 1].getTable() ) ) {
					throw new AnnotationException(
							"Mixing different tables in a property is not allowed: " + propertyName
					);
				}
			}
		}

	}

	public void addIndex(Index index, boolean inSecondPass) {
		if ( index == null ) return;
		String indexName = index.name();
		addIndex( indexName, inSecondPass );
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
		return "Ejb3Column" + "{table=" + getTable()
				+ ", mappingColumn=" + mappingColumn.getName()
				+ ", insertable=" + insertable
				+ ", updatable=" + updatable
				+ ", unique=" + unique + '}';
	}
}

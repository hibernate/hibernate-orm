/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.cfg;

import java.util.Map;

import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.ColumnTransformers;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.cfg.annotations.Nullability;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Formula;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Table;
import org.jboss.logging.Logger;

/**
 * Wrap state of an EJB3 @Column annotation
 * and build the Hibernate column mapping element
 *
 * @author Emmanuel Bernard
 */
public class Ejb3Column {

    private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, Ejb3Column.class.getName());

	private Column mappingColumn;
	private boolean insertable = true;
	private boolean updatable = true;
	private String explicitTableName;
	protected Map<String, Join> joins;
	protected PropertyHolder propertyHolder;
	private Mappings mappings;
	private boolean isImplicit;
	public static final int DEFAULT_COLUMN_LENGTH = 255;
	public String sqlType;
	private int length = DEFAULT_COLUMN_LENGTH;
	private int precision;
	private int scale;
	private String logicalColumnName;
	private String propertyName;
	private boolean unique;
	private boolean nullable = true;
	private String formulaString;
	private Formula formula;
	private Table table;
	private String readExpression;
	private String writeExpression;

	private String defaultValue;

	public void setTable(Table table) {
		this.table = table;
	}

	public String getLogicalColumnName() {
		return logicalColumnName;
	}

	public String getSqlType() {
		return sqlType;
	}

	public int getLength() {
		return length;
	}

	public int getPrecision() {
		return precision;
	}

	public int getScale() {
		return scale;
	}

	public boolean isUnique() {
		return unique;
	}

	public boolean isFormula() {
		return StringHelper.isNotEmpty( formulaString );
	}

	public String getFormulaString() {
		return formulaString;
	}

	/**
	 * Deprecated as this is badly named for its use.
	 *
	 * @deprecated Use {@link #getExplicitTableName} instead
	 */
	@Deprecated
	public String getSecondaryTableName() {
		return explicitTableName;
	}

	public String getExplicitTableName() {
		return explicitTableName;
	}

	/**
	 * Deprecated as this is badly named for its use.
	 *
	 * @deprecated Use {@link #setExplicitTableName} instead
	 */
	@Deprecated
	public void setSecondaryTableName(String explicitTableName) {
		setExplicitTableName( explicitTableName );
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

	protected Mappings getMappings() {
		return mappings;
	}

	public void setMappings(Mappings mappings) {
		this.mappings = mappings;
	}

	public void setImplicit(boolean implicit) {
		isImplicit = implicit;
	}

	public void setSqlType(String sqlType) {
		this.sqlType = sqlType;
	}

	public void setLength(int length) {
		this.length = length;
	}

	public void setPrecision(int precision) {
		this.precision = precision;
	}

	public void setScale(int scale) {
		this.scale = scale;
	}

	public void setLogicalColumnName(String logicalColumnName) {
		this.logicalColumnName = logicalColumnName;
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
		return mappingColumn.isNullable();
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	public Ejb3Column() {
	}

	public void bind() {
		if ( StringHelper.isNotEmpty( formulaString ) ) {
			LOG.debugf( "Binding formula %s", formulaString );
			formula = new Formula();
			formula.setFormula( formulaString );
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
			String columnName,
			String propertyName,
			int length,
			int precision,
			int scale,
			boolean nullable,
			String sqlType,
			boolean unique,
			boolean applyNamingStrategy) {
		if ( StringHelper.isNotEmpty( formulaString ) ) {
			this.formula = new Formula();
			this.formula.setFormula( formulaString );
		}
		else {
			this.mappingColumn = new Column();
			redefineColumnName( columnName, propertyName, applyNamingStrategy );
			this.mappingColumn.setLength( length );
			if ( precision > 0 ) {  //revelent precision
				this.mappingColumn.setPrecision( precision );
				this.mappingColumn.setScale( scale );
			}
			this.mappingColumn.setNullable( nullable );
			this.mappingColumn.setSqlType( sqlType );
			this.mappingColumn.setUnique( unique );

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
		return mappingColumn == null || StringHelper.isEmpty( mappingColumn.getName() );
	}

	public void redefineColumnName(String columnName, String propertyName, boolean applyNamingStrategy) {
		if ( applyNamingStrategy ) {
			if ( StringHelper.isEmpty( columnName ) ) {
				if ( propertyName != null ) {
					mappingColumn.setName(
							mappings.getObjectNameNormalizer().normalizeIdentifierQuoting(
									mappings.getNamingStrategy().propertyToColumnName( propertyName )
							)
					);
				}
				//Do nothing otherwise
			}
			else {
				columnName = mappings.getObjectNameNormalizer().normalizeIdentifierQuoting( columnName );
				columnName = mappings.getNamingStrategy().columnName( columnName );
				columnName = mappings.getObjectNameNormalizer().normalizeIdentifierQuoting( columnName );
				mappingColumn.setName( columnName );
			}
		}
		else {
			if ( StringHelper.isNotEmpty( columnName ) ) {
				mappingColumn.setName( mappings.getObjectNameNormalizer().normalizeIdentifierQuoting( columnName ) );
			}
		}
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
			getMappingColumn().setValue( value );
			value.addColumn( getMappingColumn() );
			value.getTable().addColumn( getMappingColumn() );
			addColumnBinding( value );
			table = value.getTable();
		}
	}

	protected void addColumnBinding(SimpleValue value) {
		String logicalColumnName = mappings.getNamingStrategy()
				.logicalColumnName( this.logicalColumnName, propertyName );
		mappings.addColumnBinding( logicalColumnName, getMappingColumn(), value.getTable() );
	}

	/**
	 * Find appropriate table of the column.
	 * It can come from a secondary table or from the main table of the persistent class
	 *
	 * @return appropriate table
	 * @throws AnnotationException missing secondary table
	 */
	public Table getTable() {
		if ( table != null ){
			return table;
		}

		if ( isSecondary() ) {
			return getJoin().getTable();
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
			throw new AnnotationException(
					"Cannot find the expected secondary table: no "
							+ explicitTableName + " available for " + propertyHolder.getClassName()
			);
		}
		else {
			return join;
		}
	}

	public void forceNotNull() {
		mappingColumn.setNullable( false );
	}

	public static Ejb3Column[] buildColumnFromAnnotation(
			javax.persistence.Column[] anns,
			org.hibernate.annotations.Formula formulaAnn,
			Nullability nullability,
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			Map<String, Join> secondaryTables,
			Mappings mappings){
		return buildColumnFromAnnotation(
				anns, formulaAnn, nullability, propertyHolder, inferredData, null, secondaryTables, mappings
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
			Mappings mappings) {
		Ejb3Column[] columns;
		if ( formulaAnn != null ) {
			Ejb3Column formulaColumn = new Ejb3Column();
			formulaColumn.setFormula( formulaAnn.value() );
			formulaColumn.setImplicit( false );
			formulaColumn.setMappings( mappings );
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
						mappings
				);
			}
			else {
				final int length = actualCols.length;
				columns = new Ejb3Column[length];
				for (int index = 0; index < length; index++) {
					final ObjectNameNormalizer nameNormalizer = mappings.getObjectNameNormalizer();
					javax.persistence.Column col = actualCols[index];
					final String sqlType = col.columnDefinition().equals( "" )
							? null
							: nameNormalizer.normalizeIdentifierQuoting( col.columnDefinition() );
					final String tableName = ! StringHelper.isEmpty(col.table())
                                             ? nameNormalizer.normalizeIdentifierQuoting( mappings.getNamingStrategy().tableName( col.table() ) )
                                             : "";
					final String columnName = nameNormalizer.normalizeIdentifierQuoting( col.name() );
					Ejb3Column column = new Ejb3Column();

					if ( length == 1 ) {
						applyColumnDefault( column, inferredData );
					}

					column.setImplicit( false );
					column.setSqlType( sqlType );
					column.setLength( col.length() );
					column.setPrecision( col.precision() );
					column.setScale( col.scale() );
					if ( StringHelper.isEmpty( columnName ) && ! StringHelper.isEmpty( suffixForDefaultColumnName ) ) {
						column.setLogicalColumnName( inferredData.getPropertyName() + suffixForDefaultColumnName );
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
					column.setMappings( mappings );
					column.extractDataFromPropertyData(inferredData);
					column.bind();
					columns[index] = column;
				}
			}
		}
		return columns;
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
		String nonNullLogicalColumnName = logicalColumnName != null ? logicalColumnName : ""; //use the default for annotations
		if ( annotation != null &&
				( StringHelper.isEmpty( annotation.forColumn() )
						|| annotation.forColumn().equals( nonNullLogicalColumnName ) ) ) {
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
			Mappings mappings) {
		Ejb3Column column = new Ejb3Column();
		Ejb3Column[] columns = new Ejb3Column[1];
		columns[0] = column;

		//not following the spec but more clean
		if ( nullability != Nullability.FORCED_NULL
				&& inferredData.getClassOrElement().isPrimitive()
				&& !inferredData.getProperty().isArray() ) {
			column.setNullable( false );
		}
		column.setLength( DEFAULT_COLUMN_LENGTH );
		final String propertyName = inferredData.getPropertyName();
		column.setPropertyName(
				BinderHelper.getRelativePath( propertyHolder, propertyName )
		);
		column.setPropertyHolder( propertyHolder );
		column.setJoins( secondaryTables );
		column.setMappings( mappings );

		// property name + suffix is an "explicit" column name
		if ( !StringHelper.isEmpty( suffixForDefaultColumnName ) ) {
			column.setLogicalColumnName( propertyName + suffixForDefaultColumnName );
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

	void addIndex(String indexName, boolean inSecondPass) {
		IndexOrUniqueKeySecondPass secondPass = new IndexOrUniqueKeySecondPass( indexName, this, mappings, false );
		if ( inSecondPass ) {
			secondPass.doSecondPass( mappings.getClasses() );
		}
		else {
			mappings.addSecondPass(
					secondPass
			);
		}
	}

	void addUniqueKey(String uniqueKeyName, boolean inSecondPass) {
		IndexOrUniqueKeySecondPass secondPass = new IndexOrUniqueKeySecondPass( uniqueKeyName, this, mappings, true );
		if ( inSecondPass ) {
			secondPass.doSecondPass( mappings.getClasses() );
		}
		else {
			mappings.addSecondPass(
					secondPass
			);
		}
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "Ejb3Column" );
		sb.append( "{table=" ).append( getTable() );
		sb.append( ", mappingColumn=" ).append( mappingColumn.getName() );
		sb.append( ", insertable=" ).append( insertable );
		sb.append( ", updatable=" ).append( updatable );
		sb.append( ", unique=" ).append( unique );
		sb.append( '}' );
		return sb.toString();
	}
}

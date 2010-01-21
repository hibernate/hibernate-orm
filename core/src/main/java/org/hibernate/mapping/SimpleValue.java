/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.mapping;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.hibernate.FetchMode;
import org.hibernate.MappingException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.Mapping;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.IdentityGenerator;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.id.factory.IdentifierGeneratorFactory;
import org.hibernate.type.Type;
import org.hibernate.type.TypeFactory;
import org.hibernate.util.ReflectHelper;

/**
 * Any value that maps to columns.
 * @author Gavin King
 */
public class SimpleValue implements KeyValue {
	public static final String DEFAULT_ID_GEN_STRATEGY = "assigned";

	private final List columns = new ArrayList();
	private String typeName;
	private Properties identifierGeneratorProperties;
	private String identifierGeneratorStrategy = DEFAULT_ID_GEN_STRATEGY;
	private String nullValue;
	private Table table;
	private String foreignKeyName;
	private boolean alternateUniqueKey;
	private Properties typeParameters;
	private boolean cascadeDeleteEnabled;

	public boolean isCascadeDeleteEnabled() {
		return cascadeDeleteEnabled;
	}

	public void setCascadeDeleteEnabled(boolean cascadeDeleteEnabled) {
		this.cascadeDeleteEnabled = cascadeDeleteEnabled;
	}
	
	public void addColumn(Column column) {
		if ( !columns.contains(column) ) columns.add(column);
		column.setValue(this);
		column.setTypeIndex( columns.size()-1 );
	}
	
	public void addFormula(Formula formula) {
		columns.add(formula);
	}
	
	public boolean hasFormula() {
		Iterator iter = getColumnIterator();
		while ( iter.hasNext() ) {
			Object o = iter.next();
			if (o instanceof Formula) return true;
		}
		return false;
	}

	public int getColumnSpan() {
		return columns.size();
	}
	public Iterator getColumnIterator() {
		return columns.iterator();
	}
	public List getConstraintColumns() {
		return columns;
	}
	public String getTypeName() {
		return typeName;
	}
	public void setTypeName(String type) {
		this.typeName = type;
	}
	public void setTable(Table table) {
		this.table = table;
	}
	
	public SimpleValue(Table table) {
		this.table = table;
	}

	public SimpleValue() {}

	public void createForeignKey() throws MappingException {}

	public void createForeignKeyOfEntity(String entityName) {
		if ( !hasFormula() && !"none".equals(getForeignKeyName())) {
			ForeignKey fk = table.createForeignKey( getForeignKeyName(), getConstraintColumns(), entityName );
			fk.setCascadeDeleteEnabled(cascadeDeleteEnabled);
		}
	}

	public IdentifierGenerator createIdentifierGenerator(
			IdentifierGeneratorFactory identifierGeneratorFactory,
			Dialect dialect, 
			String defaultCatalog, 
			String defaultSchema, 
			RootClass rootClass) throws MappingException {
		
		Properties params = new Properties();
		
		//if the hibernate-mapping did not specify a schema/catalog, use the defaults
		//specified by properties - but note that if the schema/catalog were specified
		//in hibernate-mapping, or as params, they will already be initialized and
		//will override the values set here (they are in identifierGeneratorProperties)
		if ( defaultSchema!=null ) {
			params.setProperty(PersistentIdentifierGenerator.SCHEMA, defaultSchema);
		}
		if ( defaultCatalog!=null ) {
			params.setProperty(PersistentIdentifierGenerator.CATALOG, defaultCatalog);
		}
		
		//pass the entity-name, if not a collection-id
		if (rootClass!=null) {
			params.setProperty( IdentifierGenerator.ENTITY_NAME, rootClass.getEntityName() );
		}
		
		//init the table here instead of earlier, so that we can get a quoted table name
		//TODO: would it be better to simply pass the qualified table name, instead of
		//      splitting it up into schema/catalog/table names
		String tableName = getTable().getQuotedName(dialect);
		params.setProperty( PersistentIdentifierGenerator.TABLE, tableName );
		
		//pass the column name (a generated id almost always has a single column)
		String columnName = ( (Column) getColumnIterator().next() ).getQuotedName(dialect);
		params.setProperty( PersistentIdentifierGenerator.PK, columnName );
		
		if (rootClass!=null) {
			StringBuffer tables = new StringBuffer();
			Iterator iter = rootClass.getIdentityTables().iterator();
			while ( iter.hasNext() ) {
				Table table= (Table) iter.next();
				tables.append( table.getQuotedName(dialect) );
				if ( iter.hasNext() ) tables.append(", ");
			}
			params.setProperty( PersistentIdentifierGenerator.TABLES, tables.toString() );
		}
		else {
			params.setProperty( PersistentIdentifierGenerator.TABLES, tableName );
		}

		if (identifierGeneratorProperties!=null) {
			params.putAll(identifierGeneratorProperties);
		}

		identifierGeneratorFactory.setDialect( dialect );
		return identifierGeneratorFactory.createIdentifierGenerator( identifierGeneratorStrategy, getType(), params );
		
	}

	public boolean isUpdateable() {
		//needed to satisfy KeyValue
		return true;
	}
	
	public FetchMode getFetchMode() {
		return FetchMode.SELECT;
	}

	public Properties getIdentifierGeneratorProperties() {
		return identifierGeneratorProperties;
	}

	public String getNullValue() {
		return nullValue;
	}

	public Table getTable() {
		return table;
	}

	/**
	 * Returns the identifierGeneratorStrategy.
	 * @return String
	 */
	public String getIdentifierGeneratorStrategy() {
		return identifierGeneratorStrategy;
	}
	
	public boolean isIdentityColumn(IdentifierGeneratorFactory identifierGeneratorFactory, Dialect dialect) {
		identifierGeneratorFactory.setDialect( dialect );
		return identifierGeneratorFactory.getIdentifierGeneratorClass( identifierGeneratorStrategy )
				.equals( IdentityGenerator.class );
	}

	/**
	 * Sets the identifierGeneratorProperties.
	 * @param identifierGeneratorProperties The identifierGeneratorProperties to set
	 */
	public void setIdentifierGeneratorProperties(Properties identifierGeneratorProperties) {
		this.identifierGeneratorProperties = identifierGeneratorProperties;
	}

	/**
	 * Sets the identifierGeneratorStrategy.
	 * @param identifierGeneratorStrategy The identifierGeneratorStrategy to set
	 */
	public void setIdentifierGeneratorStrategy(String identifierGeneratorStrategy) {
		this.identifierGeneratorStrategy = identifierGeneratorStrategy;
	}

	/**
	 * Sets the nullValue.
	 * @param nullValue The nullValue to set
	 */
	public void setNullValue(String nullValue) {
		this.nullValue = nullValue;
	}

	public String getForeignKeyName() {
		return foreignKeyName;
	}

	public void setForeignKeyName(String foreignKeyName) {
		this.foreignKeyName = foreignKeyName;
	}

	public boolean isAlternateUniqueKey() {
		return alternateUniqueKey;
	}

	public void setAlternateUniqueKey(boolean unique) {
		this.alternateUniqueKey = unique;
	}

	public boolean isNullable() {
		if ( hasFormula() ) return true;
		boolean nullable = true;
		Iterator iter = getColumnIterator();
		while ( iter.hasNext() ) {
			if ( !( (Column) iter.next() ).isNullable() ) {
				nullable = false;
				return nullable; //shortcut
			}
		}
		return nullable;
	}

	public boolean isSimpleValue() {
		return true;
	}

	public boolean isValid(Mapping mapping) throws MappingException {
		return getColumnSpan()==getType().getColumnSpan(mapping);
	}

	public Type getType() throws MappingException {
		if (typeName==null) {
			throw new MappingException("No type name");
		}
		Type result = TypeFactory.heuristicType(typeName, typeParameters);
		if (result==null) {
			String msg = "Could not determine type for: " + typeName;
			if(table != null){
				msg += ", at table: " + table.getName();
			}
			if(columns!=null && columns.size()>0) {
				msg += ", for columns: " + columns;
			}
			throw new MappingException(msg);
		}
		return result;
	}

	public void setTypeUsingReflection(String className, String propertyName) throws MappingException {
		if (typeName==null) {
			if (className==null) {
				throw new MappingException("you must specify types for a dynamic entity: " + propertyName);
			}
			typeName = ReflectHelper.reflectedPropertyClass(className, propertyName).getName();
		}
	}

	public boolean isTypeSpecified() {
		return typeName!=null;
	}

	public void setTypeParameters(Properties parameterMap) {
		this.typeParameters = parameterMap;
	}
	
	public Properties getTypeParameters() {
		return typeParameters;
	}

	public String toString() {
		return getClass().getName() + '(' + columns.toString() + ')';
	}

	public Object accept(ValueVisitor visitor) {
		return visitor.accept(this);
	}
	
	public boolean[] getColumnInsertability() {
		boolean[] result = new boolean[ getColumnSpan() ];
		int i = 0;
		Iterator iter = getColumnIterator();
		while ( iter.hasNext() ) {
			Selectable s = (Selectable) iter.next();
			result[i++] = !s.isFormula();
		}
		return result;
	}
	
	public boolean[] getColumnUpdateability() {
		return getColumnInsertability();
	}
}

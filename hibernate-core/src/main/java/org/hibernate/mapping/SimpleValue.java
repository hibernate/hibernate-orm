/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
package org.hibernate.mapping;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.hibernate.FetchMode;
import org.hibernate.MappingException;
import org.hibernate.cfg.AccessType;
import org.hibernate.cfg.Environment;
import org.hibernate.cfg.Mappings;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.IdentityGenerator;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.id.factory.IdentifierGeneratorFactory;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.property.DirectPropertyAccessor;
import org.hibernate.type.Type;
import org.hibernate.usertype.DynamicParameterizedType;

/**
 * Any value that maps to columns.
 * @author Gavin King
 */
public class SimpleValue implements KeyValue {
	public static final String DEFAULT_ID_GEN_STRATEGY = "assigned";

	private final Mappings mappings;

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

	public SimpleValue(Mappings mappings) {
		this.mappings = mappings;
	}

	public SimpleValue(Mappings mappings, Table table) {
		this( mappings );
		this.table = table;
	}

	public Mappings getMappings() {
		return mappings;
	}

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
			params.setProperty( IdentifierGenerator.JPA_ENTITY_NAME, rootClass.getJpaEntityName() );
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
			StringBuilder tables = new StringBuilder();
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

		// TODO : we should pass along all settings once "config lifecycle" is hashed out...
		params.put(
				Environment.PREFER_POOLED_VALUES_LO,
				mappings.getConfigurationProperties().getProperty( Environment.PREFER_POOLED_VALUES_LO, "false" )
		);

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
		if ( typeName == null ) {
			throw new MappingException( "No type name" );
		}
		if ( typeParameters != null
				&& Boolean.valueOf( typeParameters.getProperty( DynamicParameterizedType.IS_DYNAMIC ) )
				&& typeParameters.get( DynamicParameterizedType.PARAMETER_TYPE ) == null ) {
			createParameterImpl();
		}

		Type result = mappings.getTypeResolver().heuristicType( typeName, typeParameters );
		if ( result == null ) {
			String msg = "Could not determine type for: " + typeName;
			if ( table != null ) {
				msg += ", at table: " + table.getName();
			}
			if ( columns != null && columns.size() > 0 ) {
				msg += ", for columns: " + columns;
			}
			throw new MappingException( msg );
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

	@Override
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

	private void createParameterImpl() {
		try {
			String[] columnsNames = new String[columns.size()];
			for ( int i = 0; i < columns.size(); i++ ) {
				columnsNames[i] = ( (Column) columns.get( i ) ).getName();
			}

			AccessType accessType = AccessType.getAccessStrategy( typeParameters
					.getProperty( DynamicParameterizedType.ACCESS_TYPE ) );
			final Class classEntity = ReflectHelper.classForName( typeParameters
					.getProperty( DynamicParameterizedType.ENTITY ) );
			final String propertyName = typeParameters.getProperty( DynamicParameterizedType.PROPERTY );

			Annotation[] annotations;
			if ( accessType == AccessType.FIELD ) {
				annotations = ( (Field) new DirectPropertyAccessor().getGetter( classEntity, propertyName ).getMember() )
						.getAnnotations();

			}
			else {
				annotations = ReflectHelper.getGetter( classEntity, propertyName ).getMethod().getAnnotations();
			}

			typeParameters.put(
					DynamicParameterizedType.PARAMETER_TYPE,
					new ParameterTypeImpl( ReflectHelper.classForName( typeParameters
							.getProperty( DynamicParameterizedType.RETURNED_CLASS ) ), annotations, table.getCatalog(),
							table.getSchema(), table.getName(), Boolean.valueOf( typeParameters
									.getProperty( DynamicParameterizedType.IS_PRIMARY_KEY ) ), columnsNames ) );

		}
		catch ( ClassNotFoundException cnfe ) {
			throw new MappingException( "Could not create DynamicParameterizedType for type: " + typeName, cnfe );
		}
	}

	private final class ParameterTypeImpl implements DynamicParameterizedType.ParameterType {

		private final Class returnedClass;
		private final Annotation[] annotationsMethod;
		private final String catalog;
		private final String schema;
		private final String table;
		private final boolean primaryKey;
		private final String[] columns;

		private ParameterTypeImpl(Class returnedClass, Annotation[] annotationsMethod, String catalog, String schema,
				String table, boolean primaryKey, String[] columns) {
			this.returnedClass = returnedClass;
			this.annotationsMethod = annotationsMethod;
			this.catalog = catalog;
			this.schema = schema;
			this.table = table;
			this.primaryKey = primaryKey;
			this.columns = columns;
		}

		@Override
		public Class getReturnedClass() {
			return returnedClass;
		}

		@Override
		public Annotation[] getAnnotationsMethod() {
			return annotationsMethod;
		}

		@Override
		public String getCatalog() {
			return catalog;
		}

		@Override
		public String getSchema() {
			return schema;
		}

		@Override
		public String getTable() {
			return table;
		}

		@Override
		public boolean isPrimaryKey() {
			return primaryKey;
		}

		@Override
		public String[] getColumns() {
			return columns;
		}
	}
}

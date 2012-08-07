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
import javax.persistence.AttributeConverter;
import java.lang.reflect.TypeVariable;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.jboss.logging.Logger;

import org.hibernate.FetchMode;
import org.hibernate.MappingException;
import org.hibernate.cfg.AccessType;
import org.hibernate.cfg.AttributeConverterDefinition;
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
import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.java.JavaTypeDescriptorRegistry;
import org.hibernate.type.descriptor.sql.BasicBinder;
import org.hibernate.type.descriptor.sql.BasicExtractor;
import org.hibernate.type.descriptor.sql.JdbcTypeJavaClassMappings;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptorRegistry;
import org.hibernate.usertype.DynamicParameterizedType;

/**
 * Any value that maps to columns.
 * @author Gavin King
 */
public class SimpleValue implements KeyValue {
	private static final Logger log = Logger.getLogger( SimpleValue.class );

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

	private AttributeConverterDefinition jpaAttributeConverterDefinition;
	private Type type;

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
		if ( type != null ) {
			return type;
		}

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

	@SuppressWarnings("unchecked")
	public void setTypeUsingReflection(String className, String propertyName) throws MappingException {
		// NOTE : this is called as the last piece in setting SimpleValue type information, and implementations
		// rely on that fact, using it as a signal that all information it is going to get is defined at this point...

		if ( typeName != null ) {
			// assume either (a) explicit type was specified or (b) determine was already performed
			return;
		}

		if ( type != null ) {
			return;
		}

		if ( jpaAttributeConverterDefinition == null ) {
			// this is here to work like legacy.  This should change when we integrate with metamodel to
			// look for SqlTypeDescriptor and JavaTypeDescriptor individually and create the BasicType (well, really
			// keep a registry of [SqlTypeDescriptor,JavaTypeDescriptor] -> BasicType...)
			if ( className == null ) {
				throw new MappingException( "you must specify types for a dynamic entity: " + propertyName );
			}
			typeName = ReflectHelper.reflectedPropertyClass( className, propertyName ).getName();
			return;
		}

		// we had an AttributeConverter...

		// todo : we should validate the number of columns present
		// todo : ultimately I want to see attributeConverterJavaType and attributeConverterJdbcTypeCode specify-able separately
		//		then we can "play them against each other" in terms of determining proper typing
		// todo : see if we already have previously built a custom on-the-fly BasicType for this AttributeConverter; see note below about caching

		// AttributeConverter works totally in memory, meaning it converts between one Java representation (the entity
		// attribute representation) and another (the value bound into JDBC statements or extracted from results).
		// However, the Hibernate Type system operates at the lower level of actually dealing with those JDBC objects.
		// So even though we have an AttributeConverter, we still need to "fill out" the rest of the BasicType
		// data.  For the JavaTypeDescriptor portion we simply resolve the "entity attribute representation" part of
		// the AttributeConverter to resolve the corresponding descriptor.  For the SqlTypeDescriptor portion we use the
		// "database column representation" part of the AttributeConverter to resolve the "recommended" JDBC type-code
		// and use that type-code to resolve the SqlTypeDescriptor to use.
		final Class entityAttributeJavaType = jpaAttributeConverterDefinition.getEntityAttributeType();
		final Class databaseColumnJavaType = jpaAttributeConverterDefinition.getDatabaseColumnType();
		final int jdbcTypeCode = JdbcTypeJavaClassMappings.INSTANCE.determineJdbcTypeCodeForJavaClass( databaseColumnJavaType );

		final JavaTypeDescriptor javaTypeDescriptor = JavaTypeDescriptorRegistry.INSTANCE.getDescriptor( entityAttributeJavaType );
		final SqlTypeDescriptor sqlTypeDescriptor = SqlTypeDescriptorRegistry.INSTANCE.getDescriptor( jdbcTypeCode );
		// the adapter here injects the AttributeConverter calls into the binding/extraction process...
		final SqlTypeDescriptor sqlTypeDescriptorAdapter = new AttributeConverterSqlTypeDescriptorAdapter(
				jpaAttributeConverterDefinition.getAttributeConverter(),
				sqlTypeDescriptor
		);

		final String name = "BasicType adapter for AttributeConverter<" + entityAttributeJavaType + "," + databaseColumnJavaType + ">";
		type = new AbstractSingleColumnStandardBasicType( sqlTypeDescriptorAdapter, javaTypeDescriptor ) {
			@Override
			public String getName() {
				return name;
			}
		};
		log.debug( "Created : " + name );

		// todo : cache the BasicType we just created in case that AttributeConverter is applied multiple times.
	}

	private Class extractType(TypeVariable typeVariable) {
		java.lang.reflect.Type[] boundTypes = typeVariable.getBounds();
		if ( boundTypes == null || boundTypes.length != 1 ) {
			return null;
		}

		return (Class) boundTypes[0];
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

	public void setJpaAttributeConverterDefinition(AttributeConverterDefinition jpaAttributeConverterDefinition) {
		this.jpaAttributeConverterDefinition = jpaAttributeConverterDefinition;
	}

	public static class AttributeConverterSqlTypeDescriptorAdapter implements SqlTypeDescriptor {
		private final AttributeConverter converter;
		private final SqlTypeDescriptor delegate;

		public AttributeConverterSqlTypeDescriptorAdapter(AttributeConverter converter, SqlTypeDescriptor delegate) {
			this.converter = converter;
			this.delegate = delegate;
		}

		@Override
		public int getSqlType() {
			return delegate.getSqlType();
		}

		@Override
		public boolean canBeRemapped() {
			return delegate.canBeRemapped();
		}

		@Override
		public <X> ValueBinder<X> getBinder(JavaTypeDescriptor<X> javaTypeDescriptor) {
			final ValueBinder realBinder = delegate.getBinder( javaTypeDescriptor );
			return new BasicBinder<X>( javaTypeDescriptor, this ) {
				@Override
				@SuppressWarnings("unchecked")
				protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
						throws SQLException {
					realBinder.bind( st, converter.convertToDatabaseColumn( value ), index, options );
				}
			};
		}

		@Override
		public <X> ValueExtractor<X> getExtractor(JavaTypeDescriptor<X> javaTypeDescriptor) {
			final ValueExtractor realExtractor = delegate.getExtractor( javaTypeDescriptor );
			return new BasicExtractor<X>( javaTypeDescriptor, this ) {
				@Override
				@SuppressWarnings("unchecked")
				protected X doExtract(ResultSet rs, String name, WrapperOptions options) throws SQLException {
					return (X) converter.convertToEntityAttribute( realExtractor.extract( rs, name, options ) );
				}

				@Override
				@SuppressWarnings("unchecked")
				protected X doExtract(CallableStatement statement, int index, WrapperOptions options)
						throws SQLException {
					return (X) converter.convertToEntityAttribute( realExtractor.extract( statement, index, options ) );
				}

				@Override
				@SuppressWarnings("unchecked")
				protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
					return (X) converter.convertToEntityAttribute( realExtractor.extract( statement, new String[] {name}, options ) );
				}
			};
		}
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
/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

import org.hibernate.FetchMode;
import org.hibernate.MappingException;
import org.hibernate.boot.model.relational.MappedColumn;
import org.hibernate.boot.model.relational.MappedTable;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.config.spi.StandardConverters;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.IdentityGenerator;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.id.factory.IdentifierGeneratorFactory;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;
import org.hibernate.type.descriptor.spi.JdbcRecommendedSqlTypeMappingContext;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Any value that maps to columns.
 * @author Gavin King
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public abstract class SimpleValue implements KeyValue {
	private static final CoreMessageLogger log = CoreLogging.messageLogger( SimpleValue.class );

	public static final String DEFAULT_ID_GEN_STRATEGY = "assigned";

	protected final List<MappedColumn> columns = new ArrayList<>();
	protected String typeName;
	protected Properties typeParameters;
	protected MappedTable table;
	protected boolean cascadeDeleteEnabled;

	private final MetadataBuildingContext buildingContext;

	private Properties identifierGeneratorProperties;
	private String identifierGeneratorStrategy = DEFAULT_ID_GEN_STRATEGY;
	private String nullValue;
	private String foreignKeyName;
	private String foreignKeyDefinition;
	private boolean alternateUniqueKey;

	private IdentifierGenerator identifierGenerator;

	public SimpleValue(MetadataBuildingContext buildingContext, MappedTable table) {
		this.buildingContext = buildingContext;
		this.table = table;
	}

	@Override
	public MetadataBuildingContext getMetadataBuildingContext() {
		return buildingContext;
	}

	@Override
	public ServiceRegistry getServiceRegistry() {
		return buildingContext.getMetadataCollector().getMetadataBuildingOptions().getServiceRegistry();
	}

	@Override
	public boolean isCascadeDeleteEnabled() {
		return cascadeDeleteEnabled;
	}

	public void setCascadeDeleteEnabled(boolean cascadeDeleteEnabled) {
		this.cascadeDeleteEnabled = cascadeDeleteEnabled;
	}

	public void addColumn(Column column) {
		if ( getMappedColumns() != null ) {
			column.setTableName( getMappedTable().getNameIdentifier() );
		}
		if ( !columns.contains( column ) ) {
			columns.add( column );
		}
	}

	public void addFormula(Formula formula) {
		columns.add( formula );
	}

	@Override
	public boolean hasFormula() {
		for ( MappedColumn column : getMappedColumns() ) {
			if ( column.isFormula() ) {
				return true;
			}
		}
		return false;
	}

	@Override
	public int getColumnSpan() {
		return columns.size();
	}

	@Override
	public List<MappedColumn> getMappedColumns() {
		return Collections.unmodifiableList( columns );
	}

	public List<MappedColumn> getConstraintColumns() {
		return columns;
	}

	public String getTypeName() {
		return typeName;
	}

	public void setExplicitTypeName(String typeName) {
		this.typeName = typeName;
	}

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

	@Override
	public ForeignKey createForeignKeyOfEntity(String entityName) {
		// the plan here is to generate all logical ForeignKeys, but disable it for export
		// 		if it has formulas

		final ForeignKey fk = (ForeignKey) table.createForeignKey(
				getForeignKeyName(),
				getConstraintColumns(),
				entityName,
				getForeignKeyDefinition()
		);
		fk.setCascadeDeleteEnabled( cascadeDeleteEnabled );

		if ( hasFormula() || "none".equals( getForeignKeyName() ) ) {
			fk.disableCreation();
		}

		return fk;
	}

	@Override
	public IdentifierGenerator createIdentifierGenerator(
			IdentifierGeneratorFactory identifierGeneratorFactory,
			Dialect dialect,
			String defaultCatalog,
			String defaultSchema,
			RootClass rootClass) throws MappingException {

		if ( identifierGenerator != null ) {
			return identifierGenerator;
		}

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
		String tableName = getMappedTable().getQuotedName(dialect);
		params.setProperty( PersistentIdentifierGenerator.TABLE, tableName );

		//pass the column name (a generated id almost always has a single column)
		String columnName = ( (Column) getMappedColumns().get( 0 ) ).getQuotedName(dialect);
		params.setProperty( PersistentIdentifierGenerator.PK, columnName );

		if (rootClass!=null) {
			final StringBuilder tables = new StringBuilder();
			final Iterator iter = rootClass.getIdentityTables().iterator();
			while ( iter.hasNext() ) {
				final Table table= (Table) iter.next();
				tables.append( table.getQuotedName(dialect) );
				if ( iter.hasNext() ) {
					tables.append(", ");
				}
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
		final ConfigurationService cs = getServiceRegistry().getService( ConfigurationService.class );

		params.put(
				AvailableSettings.PREFER_POOLED_VALUES_LO,
				cs.getSetting( AvailableSettings.PREFER_POOLED_VALUES_LO, StandardConverters.BOOLEAN, false )
		);
		if ( cs.getSettings().get( AvailableSettings.PREFERRED_POOLED_OPTIMIZER ) != null ) {
			params.put(
					AvailableSettings.PREFERRED_POOLED_OPTIMIZER,
					cs.getSettings().get( AvailableSettings.PREFERRED_POOLED_OPTIMIZER )
			);
		}

		identifierGeneratorFactory.setDialect( dialect );
		identifierGenerator = identifierGeneratorFactory.createIdentifierGenerator(
				identifierGeneratorStrategy,
				getJavaTypeMapping().getJavaTypeDescriptor(),
				params
		);

		return identifierGenerator;
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

	@Override
	public Table getTable() {
		return (Table) getMappedTable();
	}

	@Override
	public MappedTable getMappedTable() {
		return table;
	}

	/**
	 * Returns the identifierGeneratorStrategy.
	 * @return String
	 */
	public String getIdentifierGeneratorStrategy() {
		return identifierGeneratorStrategy;
	}

	public boolean isIdentityColumn(IdentifierGeneratorFactory identifierGeneratorFactory) {
		return IdentityGenerator.class.isAssignableFrom(identifierGeneratorFactory.getIdentifierGeneratorClass( identifierGeneratorStrategy ));
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

	public String getForeignKeyDefinition() {
		return foreignKeyDefinition;
	}

	public void setForeignKeyDefinition(String foreignKeyDefinition) {
		this.foreignKeyDefinition = foreignKeyDefinition;
	}

	public boolean isAlternateUniqueKey() {
		return alternateUniqueKey;
	}

	public void setAlternateUniqueKey(boolean unique) {
		this.alternateUniqueKey = unique;
	}

	public boolean isNullable() {
		final List<MappedColumn> mappedColumns = getMappedColumns();
		for ( MappedColumn column : mappedColumns ) {
			if ( column instanceof Formula ) {
				// if there are *any* formulas, then the Value overall is
				// considered nullable
				return true;
			}
			else if ( !( (Column) column ).isNullable() ) {
				// if there is a single non-nullable column, the Value
				// overall is considered non-nullable.
				return false;
			}
		}
		// nullable by default
		return true;
	}

	public boolean isSimpleValue() {
		return true;
	}

	@Override
	public boolean isValid() throws MappingException {
		return true;
//		return getColumnSpan()==getType().getColumnSpan();
	}

	public JdbcRecommendedSqlTypeMappingContext makeJdbcRecommendedSqlTypeMappingContext(
			TypeConfiguration typeConfiguration,
			boolean isNationalized,
			boolean isLob) {
		return new LocalJdbcRecommendedSqlTypeMappingContext( typeConfiguration, isNationalized, isLob );
	}

	private class LocalJdbcRecommendedSqlTypeMappingContext implements JdbcRecommendedSqlTypeMappingContext {
		private final TypeConfiguration typeConfiguration;
		private final boolean isNationalized;
		private final boolean isLob;

		private LocalJdbcRecommendedSqlTypeMappingContext(
				TypeConfiguration typeConfiguration,
				boolean isNationalized,
				boolean isLob) {
			this.typeConfiguration = typeConfiguration;
			this.isNationalized = isNationalized;
			this.isLob = isLob;
		}

		@Override
		public boolean isNationalized() {
			return isNationalized;
		}

		@Override
		public boolean isLob() {
			return isLob;
		}

		@Override
		public TypeConfiguration getTypeConfiguration() {
			return typeConfiguration;
		}
	}

	@Override
	public void setTypeUsingReflection(String className, String propertyName) throws MappingException {
		// what to do here
		if ( typeName != null ) {
			// assume either (a) explicit type was specified or (b) determine was already performed
			return;
		}
		if ( className == null ) {
			throw new MappingException( "Attribute types for a dynamic entity must be explicitly specified: " + propertyName );
		}
		typeName = ReflectHelper.reflectedPropertyClass(
				className,
				propertyName,
				getServiceRegistry().getService( ClassLoaderService.class )
		).getName();
		// todo : to fully support isNationalized here we need do the process hinted at above
		// 		essentially, much of the logic from #buildAttributeConverterTypeAdapter wrt resolving
		//		a (1) SqlTypeDescriptor, a (2) JavaTypeDescriptor and dynamically building a BasicType
		// 		combining them.
	}

	public boolean isTypeSpecified() {
		return typeName != null;
	}

	public void setTypeParameters(Properties parameterMap) {
		this.typeParameters = parameterMap;
	}

	public Properties getTypeParameters() {
		return typeParameters;
	}

	public void copyTypeFrom( SimpleValue sourceValue ) {
		setExplicitTypeName( sourceValue.getTypeName() );
		setTypeParameters( sourceValue.getTypeParameters() );
	}

	@Override
	public String toString() {
		return getClass().getName() + '(' + columns.toString() + ')';
	}

	public boolean[] getColumnInsertability() {
		final boolean[] columnInsertability = new boolean[ getColumnSpan() ];
		int i = 0;
		for(MappedColumn column : columns){
			columnInsertability[i++] = !column.isFormula();
		}
		return columnInsertability;
	}

	public boolean[] getColumnUpdateability() {
		return getColumnInsertability();
	}

	public interface TypeDescriptorResolver {
		SqlTypeDescriptor resolveSqlTypeDescriptor();
		JavaTypeDescriptor resolveJavaTypeDescriptor();
	}

	public interface BasicTypeDescriptorResolver extends TypeDescriptorResolver {
		SqlTypeDescriptor resolveSqlTypeDescriptor();

		@Override
		BasicJavaDescriptor resolveJavaTypeDescriptor();
	}


	@Override
	public boolean isSame(Value other) {
		return this == other || other instanceof SimpleValue && isSame( (SimpleValue) other );
	}

	protected static boolean isSame(Value v1, Value v2) {
		return v1 == v2 || v1 != null && v2 != null && v1.isSame( v2 );
	}

	public boolean isSame(SimpleValue other) {
		return Objects.equals( columns, other.columns )
				&& Objects.equals( typeName, other.typeName )
				&& Objects.equals( typeParameters, other.typeParameters )
				&& Objects.equals( table, other.table )
				&& Objects.equals( foreignKeyName, other.foreignKeyName )
				&& Objects.equals( foreignKeyDefinition, other.foreignKeyDefinition );
	}
}

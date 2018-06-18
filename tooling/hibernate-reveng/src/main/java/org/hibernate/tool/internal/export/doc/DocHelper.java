package org.hibernate.tool.internal.export.doc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.Value;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.internal.export.common.ConfigurationNavigator;
import org.hibernate.tool.internal.export.pojo.Cfg2JavaTool;
import org.hibernate.tool.internal.export.pojo.ComponentPOJOClass;
import org.hibernate.tool.internal.export.pojo.POJOClass;
import org.hibernate.type.Type;

/**
 * This helper class is used expose hibernate mapping information to the
 * templates.
 * 
 * @author Ricardo C. Moral
 * @author <a href="mailto:abhayani@jboss.org">Amit Bhayani</a>
 */
public final class DocHelper {

	/** used to sort pojoclass according to their declaration name */
	static final Comparator<POJOClass> POJOCLASS_COMPARATOR = new Comparator<POJOClass>() {
		public int compare(POJOClass left, POJOClass right) {
			return left.getDeclarationName().compareTo(right.getDeclarationName());
		}
	};

	/**
	 * Used to sort properties according to their name.
	 */
	private static final Comparator<Property> PROPERTY_COMPARATOR = new Comparator<Property>() {
		public int compare(Property left, Property right) {
			return left.getName().compareTo(right.getName());
		}
	};

	/**
	 * Name to use if the schema is not specified.
	 */
	public static final String DEFAULT_NO_SCHEMA_NAME = "default";

	/**
	 * Name to use if there are no packages specified for any class
	 */
	public static final String DEFAULT_NO_PACKAGE = "All Entities";

	/**
	 * Map with Tables keyed by Schema FQN. The keys are Strings and the values
	 * are Lists of Tables
	 */
	private Map<String, List<Table>> tablesBySchema = 
			new HashMap<String, List<Table>>();

	/**
	 * Map with classes keyed by package name. PackageName is String key and
	 * values are List of POJOClass
	 */
	private Map<String, List<POJOClass>> classesByPackage = 
			new HashMap<String, List<POJOClass>>();

	/**
	 * Lits of all POJOClass
	 */
	private List<POJOClass> classes = 
			new ArrayList<POJOClass>();

	/**
	 * Map where the keys are column names (tableFQN.column) and the values are
	 * lists with the Value instances where those columns referenced.
	 */
	private Map<String, List<Value>> valuesByColumn = 
			new HashMap<String, List<Value>>();

	/**
	 * Holds intances of Property keyed by Value objects.
	 */
	private Map<Value, List<Property>> propsByValue = 
			new HashMap<Value, List<Property>>();

	/**
	 * List with all the tables.
	 */
	private List<Table> tables = new ArrayList<Table>();

	/**
	 * Map that holds the Schema FQN for each Table. The keys are Table
	 * instances and the values are Strings with the Schema FQN for that table.
	 */
	private Map<Table, String> tableSchemaNames = new HashMap<Table, String>();

	/**
	 * The Dialect.
	 */
	private Dialect dialect;

	/**
	 * Constructor.
	 * 
	 * @param cfg
	 *            Hibernate configuration.
	 */
	@SuppressWarnings("unchecked")
	public DocHelper(Metadata metadata, Properties properties, Cfg2JavaTool cfg2JavaTool) {

		super();

		if (metadata == null) {
			throw new IllegalArgumentException("Hibernate Configuration cannot be null");
		}

		StandardServiceRegistryBuilder builder = new StandardServiceRegistryBuilder();
		builder.applySettings(properties);
		ServiceRegistry serviceRegistry = builder.build();
		JdbcServices jdbcServices = serviceRegistry.getService(JdbcServices.class);
		dialect = jdbcServices.getDialect(); 
		String defaultCatalog = properties.getProperty(AvailableSettings.DEFAULT_CATALOG);
		String defaultSchema = properties.getProperty(AvailableSettings.DEFAULT_SCHEMA);
		if (defaultSchema == null) {
			defaultSchema = DEFAULT_NO_SCHEMA_NAME;
		}
		
		Iterator<Table> tablesIter = metadata.collectTableMappings().iterator();

		while (tablesIter.hasNext()) {
			Table table = tablesIter.next();

			if (!table.isPhysicalTable()) {
				continue;
			}
			tables.add(table);

			StringBuffer sb = new StringBuffer();

			String catalog = table.getCatalog();
			if (catalog == null) {
				catalog = defaultCatalog;
			}
			if (catalog != null) {
				sb.append(catalog + ".");
			}

			String schema = table.getSchema();
			if (schema == null) {
				schema = defaultSchema;
			}

			sb.append(schema);

			String qualSchemaName = sb.toString();

			tableSchemaNames.put(table, qualSchemaName);

			List<Table> tableList = tablesBySchema.get(qualSchemaName);
			if (tableList == null) {
				tableList = new ArrayList<Table>();
				tablesBySchema.put(qualSchemaName, tableList);
			}
			tableList.add(table);

			Iterator<Column> columns = table.getColumnIterator();
			while (columns.hasNext()) {
				Column column = columns.next();
				String columnFQN = getQualifiedColumnName(table, column);
				List<Value> values = valuesByColumn.get(columnFQN);
				if (values == null) {
					values = new ArrayList<Value>();
					valuesByColumn.put(columnFQN, values);
				}
				values.add(column.getValue());
			}
		}

		Map<String, Component> components = new HashMap<String, Component>();

		Iterator<PersistentClass> classesItr = metadata.getEntityBindings().iterator();
		while (classesItr.hasNext()) {
			PersistentClass clazz = classesItr.next();

			POJOClass pojoClazz = cfg2JavaTool.getPOJOClass(clazz);
			ConfigurationNavigator.collectComponents(components, pojoClazz);

			this.processClass(pojoClazz);

			Iterator<Property> propertyIterator = clazz.getPropertyIterator();
			while (propertyIterator.hasNext()) {
				Property property = propertyIterator.next();
				Value value = property.getValue();
				List<Property> props = propsByValue.get(value);
				if (props == null) {
					props = new ArrayList<Property>();
					propsByValue.put(value, props);
				}
				props.add(property);
			}
		}

		Iterator<Component> iterator = components.values().iterator();
		while (iterator.hasNext()) {
			Component component = (Component) iterator.next();
			ComponentPOJOClass element =
					new ComponentPOJOClass(component, cfg2JavaTool);
			this.processClass(element);
		}
	}

	/**
	 * Populate classes List and classesByPackage Map
	 * 
	 * @param pojoClazz
	 */
	private void processClass(POJOClass pojoClazz) {

		classes.add(pojoClazz);
		String packageName = pojoClazz.getPackageName();

		if ("".equals(packageName)) {
			packageName = DEFAULT_NO_PACKAGE;
		}

		List<POJOClass> classList = classesByPackage.get(packageName);
		if (classList == null) {
			classList = new ArrayList<POJOClass>();
			classesByPackage.put(packageName, classList);
		}
		classList.add(pojoClazz);
	}

	/**
	 * Return a Map with the tables keyed by Schema. The keys are the schema
	 * names and the values are Lists of tables.
	 * 
	 * @return a Map with the tables keyed by Schema Name.
	 */
	public Map<String, List<Table>> getTablesBySchema() {
		return tablesBySchema;
	}

	/**
	 * return a Map which has List of POJOClass as value keyed by package name
	 * as String.
	 * 
	 * @return
	 */
	public Map<String, List<POJOClass>> getClassesByPackage() {
		return classesByPackage;
	}

	/**
	 * Returns a list with all the schemas.
	 * 
	 * @return a list with all the schemas.
	 */
	public List<String> getSchemas() {
		List<String> schemas = new ArrayList<String>(tablesBySchema.keySet());
		Collections.sort(schemas);
		return schemas;
	}

	/**
	 * Return a sorted List of packages
	 * 
	 * @return
	 */
	public List<String> getPackages() {
		List<String> packages = new ArrayList<String>(classesByPackage.keySet());
		Collections.sort(packages);
		return packages;
	}

	/**
	 * Return the list of tables for a particular schema.
	 * 
	 * @param schema
	 *            the name of the schema.
	 * 
	 * @return a list with all the tables.
	 */
	public List<Table> getTables(String schema) {
		List<Table> list = tablesBySchema.get(schema);
		return list;
	}

	/**
	 * return a sorted List of POJOClass corresponding to packageName passed
	 * 
	 * @param packageName
	 *            packageName other than DEFAULT_NO_PACKAGE
	 * @return a sorted List of POJOClass
	 */
	public List<POJOClass> getClasses(String packageName) {
		List<POJOClass> clazzes = classesByPackage.get(packageName);
		List<POJOClass> orderedClasses = new ArrayList<POJOClass>(clazzes);
		Collections.sort(orderedClasses, POJOCLASS_COMPARATOR);
		return orderedClasses;
	}

	/**
	 * Return all the tables.
	 * 
	 * @return all the tables.
	 */
	public List<Table> getTables() {
		return tables;
	}

	/**
	 * Return a sorted List of all POJOClass
	 * 
	 * @return
	 */
	public List<POJOClass> getClasses() {
		List<POJOClass> orderedClasses = new ArrayList<POJOClass>(classes);
		Collections.sort(orderedClasses, POJOCLASS_COMPARATOR);
		return orderedClasses;
	}

	/**
	 * Returns the qualified schema name for a table. The qualified schema name
	 * will include the catalog name if one is specified.
	 * 
	 * @param table
	 *            the table.
	 * 
	 * @return the qualified schema name for the table.
	 */
	public String getQualifiedSchemaName(Table table) {

		return (String) tableSchemaNames.get(table);
	}

	/**
	 * Returns the qualified name of a table.
	 * 
	 * @param table
	 *            the table.
	 * 
	 * @return the qualified name of the table.
	 */
	public String getQualifiedTableName(Table table) {

		String qualifiedSchemaName = getQualifiedSchemaName(table);

		return qualifiedSchemaName + '.' + table.getName();
	}

	public String getPropertyType(Property p) {
		Value v = p.getValue();
		Type t;
		String propertyString = "N/D";
		try {
			t = v.getType();
			propertyString = t.getReturnedClass().getName();

		} catch (Exception ex) {
			// TODO we should try to get the value from value here
			// Eat Exception??
		}

		return propertyString;
	}

	/**
	 * Returns the qualified name of a column.
	 * 
	 * @param table
	 *            the table.
	 * @param column
	 *            the column
	 * 
	 * @return the FQN of the column.
	 */
	public String getQualifiedColumnName(Table table, Column column) {
		String qualifiedTableName = getQualifiedTableName(table);
		return qualifiedTableName + '.' + column.getName();
	}

	/**
	 * Get the SQL type name for a column.
	 * 
	 * @param column
	 *            the column.
	 * 
	 * @return a String with the SQL type name.
	 */
	public String getSQLTypeName(Column column) {

		try {
			return column.getSqlType(dialect, null);
		} catch (HibernateException ex) {

			// TODO: Fix this when we find a way to get the type or
			// the mapping.

			return "N/D";
		}
	}

	/**
	 * Returns the values that use the specified column.
	 * 
	 * @param table
	 *            the table.
	 * @param column
	 *            the column.
	 * 
	 * @return a list with the values.
	 */
	public List<Value> getValues(Table table, Column column) {
		String columnFQN = getQualifiedColumnName(table, column);
		List<Value> values = valuesByColumn.get(columnFQN);
		if (values != null) {
			return values;
		} else {
			return new ArrayList<Value>();
		}
	}

	/**
	 * Returns the properties that map to a column.
	 * 
	 * @param table
	 *            the table.
	 * @param column
	 *            the column.
	 * 
	 * @return a list of properties.
	 */
	public List<Property> getProperties(Table table, Column column) {

		List<Property> result = new ArrayList<Property>();
		Iterator<Value> values = getValues(table, column).iterator();
		while (values.hasNext()) {
			Value value = values.next();
			List<Property> props = propsByValue.get(value);
			if (props != null) {
				result.addAll(props);
			}
		}
		return result;
	}

	/**
	 * Method used in class.vm template to get the ComponentPOJO class
	 * corresponding to Property if its of Type Component.
	 * 
	 * @param property
	 *            Get ComponentPOJO corresponding to this Property
	 * @return POJOClass for Property
	 */
	// TODO We haven't taken into account Array?
	public POJOClass getComponentPOJO(Property property) {
		if (property.getValue() instanceof Component) {
			Component comp = (Component) property.getValue();
			ComponentPOJOClass componentPOJOClass = new ComponentPOJOClass(comp, new Cfg2JavaTool());
			return componentPOJOClass;
		} else {
			return null;
		}
	}

	public List<POJOClass> getInheritanceHierarchy(POJOClass pc) {
		if (pc.isSubclass()) {
			List<POJOClass> superClasses = new ArrayList<POJOClass>();
			POJOClass superClass = pc.getSuperClass();
			while (superClass != null) {
				superClasses.add(superClass);
				superClass = superClass.getSuperClass();
			}
			return superClasses;
		} else {
			return Collections.emptyList();
		}
	}

	public List<Property> getOrderedProperties(POJOClass pojoClass) {
		List<Property> orderedProperties = getAllProperties(pojoClass);
		Collections.sort(orderedProperties, PROPERTY_COMPARATOR);

		return orderedProperties;
	}

	public List<Property> getSimpleProperties(POJOClass pojoClass) {
		List<Property> properties = getAllProperties(pojoClass);
		if (pojoClass.hasIdentifierProperty())
			properties.remove(pojoClass.getIdentifierProperty());
		// TODO: do we need to also remove component id properties?
		if (pojoClass.hasVersionProperty())
			properties.remove(pojoClass.getVersionProperty());
		return properties;
	}

	public List<Property> getOrderedSimpleProperties(POJOClass pojoClass) {
		List<Property> orderedProperties = getSimpleProperties(pojoClass);
		Collections.sort(orderedProperties, PROPERTY_COMPARATOR);
		return orderedProperties;
	}

	private List<Property> getAllProperties(POJOClass pojoClass) {
		List<Property> properties = new ArrayList<Property>();
		for (Iterator<Property> iterator = pojoClass.getAllPropertiesIterator(); iterator.hasNext();)
			properties.add(iterator.next());
		return properties;
	}
	
}

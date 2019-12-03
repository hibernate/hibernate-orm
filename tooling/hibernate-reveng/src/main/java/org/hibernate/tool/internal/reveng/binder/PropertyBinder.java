package org.hibernate.tool.internal.reveng.binder;

import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.MetaAttribute;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.Value;
import org.hibernate.tool.api.reveng.ReverseEngineeringStrategy;
import org.hibernate.tool.api.reveng.TableIdentifier;
import org.hibernate.tool.internal.reveng.RevEngUtils;

public class PropertyBinder {

	private static final Logger LOGGER = Logger.getLogger(PropertyBinder.class.getName());
	
	static PropertyBinder create(BinderContext binderContext) {
		return new PropertyBinder(binderContext);
	}

	private final ReverseEngineeringStrategy revengStrategy;
	private final String defaultCatalog;
	private final String defaultSchema;
	
	private PropertyBinder(BinderContext binderContext) {
		this.revengStrategy = binderContext.revengStrategy;
		this.defaultCatalog = binderContext.properties.getProperty(AvailableSettings.DEFAULT_CATALOG);
		this.defaultSchema = binderContext.properties.getProperty(AvailableSettings.DEFAULT_SCHEMA);
	}

	public Property bind(
			Table table, 
			String propertyName, 
			Value value, 
			boolean insertable, 
			boolean updatable, 
			boolean lazy, 
			String cascade, 
			String propertyAccessorName) {
    	LOGGER.log(Level.INFO, "Building property " + propertyName);
        Property prop = new Property();
		prop.setName(propertyName);
		prop.setValue(value);
		prop.setInsertable(insertable);
		prop.setUpdateable(updatable);
		prop.setLazy(lazy);
		prop.setCascade(cascade==null?"none":cascade);
		prop.setPropertyAccessorName(propertyAccessorName==null?"property":propertyAccessorName);
		return bindMetaAttributes(prop, table);
	}

    private Property bindMetaAttributes(Property property, Table table) {
    	Iterator<Selectable> columnIterator = property.getValue().getColumnIterator();
		while(columnIterator.hasNext()) {
			Column col = (Column) columnIterator.next();
			Map<String,MetaAttribute> map = getColumnToMetaAttributesInRevengStrategy(table, col.getName());
			if(map!=null) { 
				property.setMetaAttributes(map);
			}
		}

		return property;
    }

	private Map<String,MetaAttribute> getColumnToMetaAttributesInRevengStrategy(
			Table table,
			String column) {
		Map<String,MetaAttribute> result = null;
		TableIdentifier tableIdentifier = TableIdentifier.create(table);
		result = revengStrategy.columnToMetaAttributes(tableIdentifier, column);
		if (result == null) {
			String catalog = RevEngUtils.getCatalogForModel(table.getCatalog(), defaultCatalog);
			String schema = RevEngUtils.getSchemaForModel(table.getSchema(), defaultSchema);
			tableIdentifier = new TableIdentifier(catalog, schema, table.getName());
			result = revengStrategy.columnToMetaAttributes(tableIdentifier, column);
		}
		return result;
	}
	
}

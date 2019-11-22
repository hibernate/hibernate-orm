package org.hibernate.tool.internal.reveng.binder;

import java.util.Iterator;
import java.util.Map;

import org.hibernate.mapping.Column;
import org.hibernate.mapping.MetaAttribute;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.Table;
import org.hibernate.tool.api.reveng.ReverseEngineeringStrategy;
import org.hibernate.tool.api.reveng.TableIdentifier;
import org.hibernate.tool.internal.reveng.RevEngUtils;

public class MetaAttributesBinder {

    public static Property bindMetaAttributes(
    		Property property, 
    		ReverseEngineeringStrategy revengStrategy,
    		Table table,
    		String defaultCatalog,
    		String defaultSchema) {
    	Iterator<Selectable> columnIterator = property.getValue().getColumnIterator();
		while(columnIterator.hasNext()) {
			Column col = (Column) columnIterator.next();
			Map<String,MetaAttribute> map = getColumnToMetaAttributesInRevengStrategy(
					revengStrategy, 
					table, 
					defaultCatalog, 
					defaultSchema, 
					col.getName());
			if(map!=null) { 
				property.setMetaAttributes(map);
			}
		}

		return property;
    }

	private static Map<String,MetaAttribute> getColumnToMetaAttributesInRevengStrategy(
			ReverseEngineeringStrategy revengStrat,
			Table table,
			String defaultCatalog,
			String defaultSchema,
			String column) {
		Map<String,MetaAttribute> result = null;
		TableIdentifier tableIdentifier = TableIdentifier.create(table);
		result = revengStrat.columnToMetaAttributes(tableIdentifier, column);
		if (result == null) {
			String catalog = RevEngUtils.getCatalogForModel(table.getCatalog(), defaultCatalog);
			String schema = RevEngUtils.getSchemaForModel(table.getSchema(), defaultSchema);
			tableIdentifier = new TableIdentifier(catalog, schema, table.getName());
			result = revengStrat.columnToMetaAttributes(tableIdentifier, column);
		}
		return result;
	}
	
}

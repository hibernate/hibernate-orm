package org.hibernate.tool.internal.reveng;

import java.util.Iterator;
import java.util.Map;

import org.hibernate.mapping.Column;
import org.hibernate.mapping.MetaAttribute;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.Table;
import org.hibernate.tool.api.reveng.ReverseEngineeringStrategy;

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
			Map<String,MetaAttribute> map = RevEngUtils.getColumnToMetaAttributesInRevengStrategy(
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

}

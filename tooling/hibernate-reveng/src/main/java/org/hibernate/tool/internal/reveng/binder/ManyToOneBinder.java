package org.hibernate.tool.internal.reveng.binder;

import java.util.Iterator;
import java.util.Set;

import org.hibernate.FetchMode;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;
import org.hibernate.tool.api.reveng.ReverseEngineeringStrategy;

public class ManyToOneBinder {
	
	public static ManyToOneBinder create(BinderContext binderContext) {
		return new ManyToOneBinder(binderContext);
	}
	
	private final MetadataBuildingContext metadataBuildingContext;
	private final ReverseEngineeringStrategy revengStrategy;
	private final String defaultCatalog;
	private final String defaultSchema;
	
	private ManyToOneBinder(BinderContext binderContext) {
		this.metadataBuildingContext = binderContext.metadataBuildingContext;
		this.revengStrategy = binderContext.revengStrategy;
		this.defaultCatalog = binderContext.properties.getProperty(AvailableSettings.DEFAULT_CATALOG);
		this.defaultSchema = binderContext.properties.getProperty(AvailableSettings.DEFAULT_SCHEMA);
	}

    public Property bind(
    		String propertyName, 
    		boolean mutable, 
    		Table table, 
    		ForeignKey fk, 
    		Set<Column> processedColumns) {
    	
        ManyToOne value = new ManyToOne(metadataBuildingContext, table);
        value.setReferencedEntityName( fk.getReferencedEntityName() );
		Iterator<Column> columns = fk.getColumnIterator();
        while ( columns.hasNext() ) {
			Column fkcolumn = (Column) columns.next();
			BinderUtils.checkColumnForMultipleBinding(fkcolumn);
            value.addColumn(fkcolumn);
            processedColumns.add(fkcolumn);
		}
        value.setFetchMode(FetchMode.SELECT);

        return EntityPropertyBinder
        		.create(
        				revengStrategy, 
        				defaultCatalog, 
        				defaultSchema)
        		.bind(
        				propertyName, 
        				mutable, 
        				table, 
        				fk, 
        				value, 
        				false);
     }

}

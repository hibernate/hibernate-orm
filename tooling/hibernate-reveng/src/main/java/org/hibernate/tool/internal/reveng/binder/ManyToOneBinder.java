package org.hibernate.tool.internal.reveng.binder;

import java.util.Iterator;
import java.util.Set;

import org.hibernate.FetchMode;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;

public class ManyToOneBinder {
	
	public static ManyToOneBinder create(BinderContext binderContext) {
		return new ManyToOneBinder(binderContext);
	}
	
	private final MetadataBuildingContext metadataBuildingContext;
	private final EntityPropertyBinder entityPropertyBinder;
	
	private ManyToOneBinder(BinderContext binderContext) {
		this.entityPropertyBinder = EntityPropertyBinder.create(binderContext);
		this.metadataBuildingContext = binderContext.metadataBuildingContext;
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

        return entityPropertyBinder
        		.bind(
        				propertyName, 
        				mutable, 
        				table, 
        				fk, 
        				value, 
        				false);
     }

}

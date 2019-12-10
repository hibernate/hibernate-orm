package org.hibernate.tool.internal.reveng.binder;

import java.util.Iterator;
import java.util.Set;

import org.hibernate.FetchMode;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.OneToOne;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;
import org.hibernate.tool.api.reveng.TableIdentifier;
import org.hibernate.type.ForeignKeyDirection;

class OneToOneBinder extends AbstractBinder {
	
	static OneToOneBinder create(BinderContext binderContext) {
		return new OneToOneBinder(binderContext); 
	}
	
	private final EntityPropertyBinder entityPropertyBinder;
	
	private OneToOneBinder(BinderContext binderContext) {
		super(binderContext);
		this.entityPropertyBinder = EntityPropertyBinder.create(binderContext);
	}

    Property bind(
    		PersistentClass rc, 
    		Table targetTable,
            ForeignKey fk, 
            Set<Column> processedColumns, 
            boolean constrained, 
            boolean inverseProperty) {

        OneToOne value = new OneToOne(getMetadataBuildingContext(), targetTable, rc);
        value.setReferencedEntityName(
        		getRevengStrategy().tableToClassName(TableIdentifier.create(targetTable)));

        boolean isUnique = ForeignKeyUtils.isUniqueReference(fk);
        String propertyName = null;
        if(inverseProperty) {
            propertyName = getRevengStrategy().foreignKeyToInverseEntityName(
        		fk.getName(),
                TableIdentifier.create(fk.getReferencedTable()), 
                fk.getReferencedColumns(), 
                TableIdentifier.create(targetTable), 
                fk.getColumns(), 
                isUnique);
        } else {
            propertyName = getRevengStrategy().foreignKeyToEntityName(
        		fk.getName(),
                TableIdentifier.create(fk.getReferencedTable()), 
                fk.getReferencedColumns(), 
                TableIdentifier.create(targetTable), 
                fk.getColumns(), 
                isUnique);
        }

        Iterator<Column> columns = fk.getColumnIterator();
        while (columns.hasNext()) {
            Column fkcolumn = (Column) columns.next();
			BinderUtils.checkColumnForMultipleBinding(fkcolumn);
            value.addColumn(fkcolumn);
            processedColumns.add(fkcolumn);
        }

        value.setFetchMode(FetchMode.SELECT);

        value.setConstrained(constrained);
        value.setForeignKeyType( constrained ?
				ForeignKeyDirection.FROM_PARENT :
				ForeignKeyDirection.TO_PARENT );

        return entityPropertyBinder
        		.bind(
        				propertyName, 
        				true, 
        				targetTable, 
        				fk, 
        				value, 
        				inverseProperty);

    }

}

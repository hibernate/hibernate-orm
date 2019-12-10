package org.hibernate.tool.internal.reveng.binder;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.mapping.Column;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;
import org.hibernate.tool.api.reveng.TableIdentifier;

class ForeignKeyBinder extends AbstractBinder {
	
	private static Logger LOGGER = Logger.getLogger(ForeignKeyBinder.class.getName());
	
	static ForeignKeyBinder create(BinderContext binderContext) {
		return new ForeignKeyBinder(binderContext);
	}
	
	private final OneToOneBinder oneToOneBinder;
	private final OneToManyBinder oneToManyBinder;
	private final ManyToOneBinder manyToOneBinder;
	
	private ForeignKeyBinder(BinderContext binderContext) {
		super(binderContext);
		this.oneToOneBinder = OneToOneBinder.create(binderContext);
		this.oneToManyBinder = OneToManyBinder.create(binderContext);
		this.manyToOneBinder = ManyToOneBinder.create(binderContext);
	}
	
	void bindIncoming(
			ForeignKey foreignKey,
			PersistentClass persistentClass,
			Set<Column> processed) {
		if(excludeForeignKeyAsCollection(foreignKey)) {
			LOGGER.log(Level.INFO, "Rev.eng excluded one-to-many or one-to-one for foreignkey " + foreignKey.getName());
		} else if (getRevengStrategy().isOneToOne(foreignKey)){
			addOneToOne(foreignKey, persistentClass, processed, false);
		} else {
			addOneToMany(foreignKey, persistentClass);
		}		
	}
	
	void bindOutgoing(
			ForeignKey foreignKey,
			Table table,
			PersistentClass rc,
			Set<Column> processedColumns,
			boolean mutable) {
        if(excludeForeignKeyAsManyToOne(foreignKey)) {
        	// TODO: if many-to-one is excluded should the column be marked as processed so it won't show up at all ?
        	LOGGER.log(Level.INFO, "Rev.eng excluded *-to-one for foreignkey " + foreignKey.getName());
        } else if (isOneToOne(foreignKey)){
        	addOneToOne(foreignKey, rc, processedColumns, true);
		} else {
        	addManyToOne(foreignKey, table, rc, processedColumns, mutable);
        }		
	}
	
	private String getForeignKeyToEntityName(ForeignKey foreignKey) {
		return getRevengStrategy().foreignKeyToEntityName(
    			foreignKey.getName(),
    			TableIdentifier.create(foreignKey.getTable() ),
    			foreignKey.getColumns(),
    			TableIdentifier.create(foreignKey.getReferencedTable() ),
    			foreignKey.getReferencedColumns(),
    			ForeignKeyUtils.isUniqueReference(foreignKey));		
	}
	
	private void addManyToOne(
			ForeignKey foreignKey,
			Table table,
			PersistentClass rc,
			Set<Column> processedColumns,
			boolean mutable) {
    	Property property = manyToOneBinder.bind(
    			BinderUtils.makeUnique(rc, getForeignKeyToEntityName(foreignKey)), 
    			mutable, 
    			table, 
    			foreignKey, 
    			processedColumns);
    	rc.addProperty(property);
	}
	
	private void addOneToOne(
			ForeignKey foreignKey,
			PersistentClass rc,
			Set<Column> processedColumns,
			boolean outgoing) {
		Table table = outgoing ? foreignKey.getReferencedTable() : foreignKey.getTable();
    	Property property = oneToOneBinder.bind(
    			rc, 
    			table, 
    			foreignKey, 
    			processedColumns, 
    			outgoing, 
    			!outgoing);
		rc.addProperty(property);
	}
	
	private void addOneToMany(ForeignKey foreignKey, PersistentClass persistentClass) {
		persistentClass.addProperty(oneToManyBinder.bind(persistentClass, foreignKey));
	}
	
	private boolean excludeForeignKeyAsCollection(ForeignKey foreignKey) {
		return getRevengStrategy().excludeForeignKeyAsCollection(
				foreignKey.getName(),
				TableIdentifier.create(foreignKey.getTable() ),
				foreignKey.getColumns(),
				TableIdentifier.create(foreignKey.getReferencedTable() ),
				foreignKey.getReferencedColumns());
	}
	
	private boolean excludeForeignKeyAsManyToOne(ForeignKey foreignKey) {
		return getRevengStrategy().excludeForeignKeyAsManytoOne(
				foreignKey.getName(),
    			TableIdentifier.create(foreignKey.getTable() ),
    			foreignKey.getColumns(),
    			TableIdentifier.create(foreignKey.getReferencedTable()),
    			foreignKey.getReferencedColumns());
	}
	
	private boolean isOneToOne(ForeignKey foreignKey) {
		return getRevengStrategy().isOneToOne(foreignKey);
	}

}

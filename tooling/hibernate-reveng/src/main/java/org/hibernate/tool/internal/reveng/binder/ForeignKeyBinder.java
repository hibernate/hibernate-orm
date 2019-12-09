package org.hibernate.tool.internal.reveng.binder;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.engine.spi.Mapping;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;
import org.hibernate.tool.api.reveng.TableIdentifier;

public class ForeignKeyBinder extends AbstractBinder {
	
	private static Logger LOGGER = Logger.getLogger(ForeignKeyBinder.class.getName());
	
	public static ForeignKeyBinder create(BinderContext binderContext) {
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
	
	public void bindIncoming(
			ForeignKey foreignKey,
			PersistentClass persistentClass,
			Set<Column> processed,
			Mapping mapping) {
		if(getRevengStrategy().excludeForeignKeyAsCollection(
				foreignKey.getName(),
				TableIdentifier.create(foreignKey.getTable() ),
				foreignKey.getColumns(),
				TableIdentifier.create(foreignKey.getReferencedTable() ),
				foreignKey.getReferencedColumns())) {
			LOGGER.log(Level.INFO, "Rev.eng excluded one-to-many or one-to-one for foreignkey " + foreignKey.getName());
		} else if (getRevengStrategy().isOneToOne(foreignKey)){
        	Property property = oneToOneBinder
        			.bind(
        					persistentClass, 
        					foreignKey.getTable(), 
        					foreignKey, 
        					processed, 
        					false, 
        					true);
			persistentClass.addProperty(property);
		} else {
			Property property = oneToManyBinder.bind(persistentClass, foreignKey, processed);
			persistentClass.addProperty(property);
		}		
	}
	
	public void bindOutgoing(
			ForeignKey foreignKey,
			Table table,
			PersistentClass rc,
			Set<Column> processedColumns,
			boolean mutable) {

        if(getRevengStrategy().excludeForeignKeyAsManytoOne(foreignKey.getName(),
    			TableIdentifier.create(foreignKey.getTable() ),
    			foreignKey.getColumns(),
    			TableIdentifier.create(foreignKey.getReferencedTable() ),
    			foreignKey.getReferencedColumns())) {
        	// TODO: if many-to-one is excluded should the column be marked as processed so it won't show up at all ?
        	LOGGER.log(Level.INFO, "Rev.eng excluded *-to-one for foreignkey " + foreignKey.getName());
        } else if (getRevengStrategy().isOneToOne(foreignKey)){
        	Property property = oneToOneBinder
        			.bind(
        					rc, 
        					foreignKey.getReferencedTable(), 
        					foreignKey, 
        					processedColumns, 
        					true, 
        					false);
			rc.addProperty(property);
		} else {
        	boolean isUnique = ForeignKeyUtils.isUniqueReference(foreignKey);
        	String propertyName = getRevengStrategy().foreignKeyToEntityName(
        			foreignKey.getName(),
        			TableIdentifier.create(foreignKey.getTable() ),
        			foreignKey.getColumns(),
        			TableIdentifier.create(foreignKey.getReferencedTable() ),
        			foreignKey.getReferencedColumns(),
        			isUnique
        	);

        	Property property = manyToOneBinder
        			.bind(
        					BinderUtils.makeUnique(rc, propertyName), 
        					mutable, 
        					table, 
        					foreignKey, 
        					processedColumns);

        	rc.addProperty(property);
        }		
	}

}

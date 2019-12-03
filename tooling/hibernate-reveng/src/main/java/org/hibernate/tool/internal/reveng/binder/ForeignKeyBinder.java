package org.hibernate.tool.internal.reveng.binder;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;
import org.hibernate.tool.api.reveng.ReverseEngineeringStrategy;
import org.hibernate.tool.api.reveng.TableIdentifier;

public class ForeignKeyBinder {
	
	private static Logger LOGGER = Logger.getLogger(ForeignKeyBinder.class.getName());
	
	public static ForeignKeyBinder create(BinderContext binderContext) {
		return new ForeignKeyBinder(binderContext);
	}
	
	private MetadataBuildingContext metadataBuildingContext;
	private InFlightMetadataCollector metadataCollector;
	private ReverseEngineeringStrategy revengStrategy;
	private String defaultCatalog;
	private String defaultSchema;
	private final OneToOneBinder oneToOneBinder;
	
	private ForeignKeyBinder(BinderContext binderContext) {
		this.oneToOneBinder = OneToOneBinder.create(binderContext);
		this.metadataBuildingContext = binderContext.metadataBuildingContext;
		this.metadataCollector = binderContext.metadataCollector;
		this.revengStrategy = binderContext.revengStrategy;
		this.defaultCatalog = binderContext.properties.getProperty(AvailableSettings.DEFAULT_CATALOG);
		this.defaultSchema = binderContext.properties.getProperty(AvailableSettings.DEFAULT_SCHEMA);
	}
	
	public void bindIncoming(
			ForeignKey foreignKey,
			PersistentClass persistentClass,
			Set<Column> processed,
			Mapping mapping) {
		if(revengStrategy.excludeForeignKeyAsCollection(
				foreignKey.getName(),
				TableIdentifier.create(foreignKey.getTable() ),
				foreignKey.getColumns(),
				TableIdentifier.create(foreignKey.getReferencedTable() ),
				foreignKey.getReferencedColumns())) {
			LOGGER.log(Level.INFO, "Rev.eng excluded one-to-many or one-to-one for foreignkey " + foreignKey.getName());
		} else if (revengStrategy.isOneToOne(foreignKey)){
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
			Property property = OneToManyBinder
					.create(
							metadataBuildingContext, 
							metadataCollector, 
							revengStrategy, 
							defaultCatalog, 
							defaultSchema)
					.bind(
							persistentClass, 
							foreignKey, 
							processed, 
							mapping);
			persistentClass.addProperty(property);
		}		
	}
	
	public void bindOutgoing(
			ForeignKey foreignKey,
			Table table,
			PersistentClass rc,
			Set<Column> processedColumns,
			boolean mutable) {

        if(revengStrategy.excludeForeignKeyAsManytoOne(foreignKey.getName(),
    			TableIdentifier.create(foreignKey.getTable() ),
    			foreignKey.getColumns(),
    			TableIdentifier.create(foreignKey.getReferencedTable() ),
    			foreignKey.getReferencedColumns())) {
        	// TODO: if many-to-one is excluded should the column be marked as processed so it won't show up at all ?
        	LOGGER.log(Level.INFO, "Rev.eng excluded *-to-one for foreignkey " + foreignKey.getName());
        } else if (revengStrategy.isOneToOne(foreignKey)){
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
        	String propertyName = revengStrategy.foreignKeyToEntityName(
        			foreignKey.getName(),
        			TableIdentifier.create(foreignKey.getTable() ),
        			foreignKey.getColumns(),
        			TableIdentifier.create(foreignKey.getReferencedTable() ),
        			foreignKey.getReferencedColumns(),
        			isUnique
        	);

        	Property property = ManyToOneBinder
        			.create(
        					metadataBuildingContext, 
        					revengStrategy, 
        					defaultCatalog, 
        					defaultSchema)
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

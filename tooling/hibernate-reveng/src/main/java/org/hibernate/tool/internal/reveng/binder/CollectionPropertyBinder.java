package org.hibernate.tool.internal.reveng.binder;

import org.hibernate.FetchMode;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;
import org.hibernate.tool.api.reveng.AssociationInfo;
import org.hibernate.tool.api.reveng.ReverseEngineeringStrategy;

public class CollectionPropertyBinder {
	
	public static CollectionPropertyBinder create(BinderContext binderContext) {
		return new CollectionPropertyBinder(binderContext);
	}
	
	private final ReverseEngineeringStrategy revengStrategy;
	private final String defaultCatalog;
	private final String defaultSchema;
	
	private CollectionPropertyBinder(BinderContext binderContext) {
		this.revengStrategy = binderContext.revengStrategy;
		this.defaultCatalog = binderContext.properties.getProperty(AvailableSettings.DEFAULT_CATALOG);
		this.defaultSchema = binderContext.properties.getProperty(AvailableSettings.DEFAULT_SCHEMA);
	}

    public Property bind(
    		String propertyName, 
    		boolean mutable,
			Table table, 
			ForeignKey fk, 
			Collection value, 
			boolean inverseProperty) {

    	AssociationInfo fkei = inverseProperty?revengStrategy.foreignKeyToInverseAssociationInfo(fk):revengStrategy.foreignKeyToAssociationInfo(fk);

        String fetchMode = null;
        String cascade = null;
        boolean update = mutable;
        boolean insert = mutable;

        if(fkei != null){
        	cascade = fkei.getCascade();
        	if(cascade==null) cascade = "all"; //To ensure collections cascade to be compatible with Seam-gen and previous behavior.
        	if(fkei.getUpdate()!=null) {
        		update = fkei.getUpdate().booleanValue();
        	}
        	if(fkei.getInsert()!=null) {
        		insert = fkei.getInsert().booleanValue();
        	}

        	fetchMode = fkei.getFetch();


        }

        if(FetchMode.JOIN.toString().equalsIgnoreCase(fetchMode)) {
        	value.setFetchMode(FetchMode.JOIN);
        }
        else if(FetchMode.SELECT.toString().equalsIgnoreCase(fetchMode)) {
        	value.setFetchMode(FetchMode.SELECT);
        }
        else {
        	value.setFetchMode(FetchMode.SELECT);
        }

        return PropertyBinder.bind(
        		table, 
        		defaultCatalog,
        		defaultSchema,
        		propertyName, 
        		value, 
        		insert, 
        		update, 
        		value.getFetchMode()!=FetchMode.JOIN, 
        		cascade, 
        		null,
        		revengStrategy);

	}
}

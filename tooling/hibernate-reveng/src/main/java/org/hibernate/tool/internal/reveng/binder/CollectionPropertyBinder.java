package org.hibernate.tool.internal.reveng.binder;

import org.hibernate.FetchMode;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;
import org.hibernate.tool.api.reveng.AssociationInfo;

class CollectionPropertyBinder extends AbstractBinder {
	
	static CollectionPropertyBinder create(BinderContext binderContext) {
		return new CollectionPropertyBinder(binderContext);
	}
	
	private final PropertyBinder propertyBinder;
	
	private CollectionPropertyBinder(BinderContext binderContext) {
		super(binderContext);
		this.propertyBinder = PropertyBinder.create(binderContext);
	}

    Property bind(
    		String propertyName, 
    		boolean mutable,
			Table table, 
			ForeignKey fk, 
			Collection value, 
			boolean inverseProperty) {

    	AssociationInfo fkei = inverseProperty?getRevengStrategy().foreignKeyToInverseAssociationInfo(fk):getRevengStrategy().foreignKeyToAssociationInfo(fk);

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

        return propertyBinder.bind(
        		table, 
        		propertyName, 
        		value, 
        		insert, 
        		update, 
        		value.getFetchMode()!=FetchMode.JOIN, 
        		cascade, 
        		null);

	}
}

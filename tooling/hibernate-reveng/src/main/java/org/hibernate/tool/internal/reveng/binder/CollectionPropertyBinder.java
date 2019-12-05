package org.hibernate.tool.internal.reveng.binder;

import org.hibernate.FetchMode;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;
import org.hibernate.tool.api.reveng.AssociationInfo;
import org.hibernate.tool.internal.reveng.DefaultAssociationInfo;

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
    	AssociationInfo associationInfo = determineAssociationInfo(fk, inverseProperty, mutable);
    	updateFetchMode(value, associationInfo.getFetch());
        return propertyBinder.bind(
        		table, 
        		propertyName, 
        		value, 
        		associationInfo.getInsert(), 
        		associationInfo.getUpdate(), 
        		value.getFetchMode()!=FetchMode.JOIN, 
        		associationInfo.getCascade(), 
        		null);
	}
    
    private AssociationInfo determineAssociationInfo(
    		ForeignKey foreignKey, 
    		boolean inverseProperty, 
    		boolean mutable) {
    	AssociationInfo foreignKeyAssociationInfo = 
    			getAssociationInfoInRevengStrategy(foreignKey, inverseProperty);
    	DefaultAssociationInfo result = new DefaultAssociationInfo();
    	result.setFetch(null);
    	result.setCascade(null);
    	result.setUpdate(mutable);
    	result.setInsert(mutable);
    	if(foreignKeyAssociationInfo != null){
        	if (foreignKeyAssociationInfo.getCascade() == null) {
        		result.setCascade("all");
        	} else {
        		result.setCascade(foreignKeyAssociationInfo.getCascade());
        	}
        	if(foreignKeyAssociationInfo.getUpdate()!=null) {
        		result.setUpdate(foreignKeyAssociationInfo.getUpdate());;
        	} 
        	if(foreignKeyAssociationInfo.getInsert()!=null) {
        		result.setInsert(foreignKeyAssociationInfo.getInsert());
        	}
        	result.setFetch(foreignKeyAssociationInfo.getFetch());
        }
        return result;
    }
    
    private AssociationInfo getAssociationInfoInRevengStrategy(
    		ForeignKey foreignKey, 
    		boolean inverseProperty) {
    	if (inverseProperty) {
    		return getRevengStrategy().foreignKeyToInverseAssociationInfo(foreignKey);
    	} else {
    		return getRevengStrategy().foreignKeyToAssociationInfo(foreignKey);
    	}
    }
    
    private void updateFetchMode(Collection value, String fetchMode) {
        if(FetchMode.JOIN.toString().equalsIgnoreCase(fetchMode)) {
        	value.setFetchMode(FetchMode.JOIN);
        }
        else {
        	value.setFetchMode(FetchMode.SELECT);
        }    	
    }
    
}

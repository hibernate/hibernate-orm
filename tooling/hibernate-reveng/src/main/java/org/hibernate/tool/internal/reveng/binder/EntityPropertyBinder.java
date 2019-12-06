package org.hibernate.tool.internal.reveng.binder;

import org.hibernate.FetchMode;
import org.hibernate.mapping.Fetchable;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.ToOne;
import org.hibernate.tool.api.reveng.AssociationInfo;
import org.hibernate.tool.internal.reveng.DefaultAssociationInfo;

class EntityPropertyBinder extends AbstractBinder {
	
	static EntityPropertyBinder create(BinderContext binderContext) {
		return new EntityPropertyBinder(binderContext);
	}
	
	private final PropertyBinder propertyBinder;
	
	private EntityPropertyBinder(BinderContext binderContext) {
		super(binderContext);
		this.propertyBinder = PropertyBinder.create(binderContext);
	}

	Property bind(
			String propertyName, 
			boolean mutable,
			Table table, 
			ForeignKey fk, 
			ToOne value, 
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

    private DefaultAssociationInfo determineAssociationInfo(
    		ForeignKey foreignKey, 
    		boolean inverseProperty, 
    		boolean mutable) {
    	AssociationInfo foreignKeyAssociationInfo = 
    			getAssociationInfoInRevengStrategy(foreignKey, inverseProperty);
    	DefaultAssociationInfo result = DefaultAssociationInfo.create(null, null, mutable, mutable);
    	if(foreignKeyAssociationInfo != null){
        	updateAssociationInfo(foreignKeyAssociationInfo, result);
        }
        return result;
    }
    
    private void updateAssociationInfo(AssociationInfo origin, DefaultAssociationInfo target) {
    	target.setCascade(origin.getCascade());
    	if(origin.getUpdate()!=null) {
    		target.setUpdate(origin.getUpdate());;
    	} 
    	if(origin.getInsert()!=null) {
    		target.setInsert(origin.getInsert());
    	}
    	target.setFetch(origin.getFetch());
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
    
    private void updateFetchMode(Fetchable value, String fetchMode) {
        if(FetchMode.JOIN.toString().equalsIgnoreCase(fetchMode)) {
        	value.setFetchMode(FetchMode.JOIN);
        }
        else {
        	value.setFetchMode(FetchMode.SELECT);
        }    	
    }
    
}

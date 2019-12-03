package org.hibernate.tool.internal.reveng.binder;

import org.hibernate.FetchMode;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.ToOne;
import org.hibernate.tool.api.reveng.AssociationInfo;
import org.hibernate.tool.api.reveng.ReverseEngineeringStrategy;

public class EntityPropertyBinder {
	
	public static EntityPropertyBinder create(BinderContext binderContext) {
		return new EntityPropertyBinder(binderContext);
	}
	
	private final ReverseEngineeringStrategy revengStrategy;
	private final PropertyBinder propertyBinder;
	
	private EntityPropertyBinder(BinderContext binderContext) {
		this.revengStrategy = binderContext.revengStrategy;
		this.propertyBinder = PropertyBinder.create(binderContext);
	}

	public Property bind(
			String propertyName, 
			boolean mutable,
			Table table, 
			ForeignKey fk, 
			ToOne value, 
			boolean inverseProperty) {
		
		AssociationInfo fkei = 
				inverseProperty ? 
						revengStrategy.foreignKeyToInverseAssociationInfo(fk) : 
							revengStrategy.foreignKeyToAssociationInfo(fk);

        String fetchMode = null;
        String cascade = null;
        boolean update = mutable;
        boolean insert = mutable;

        if(fkei != null){
        	cascade = fkei.getCascade();
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

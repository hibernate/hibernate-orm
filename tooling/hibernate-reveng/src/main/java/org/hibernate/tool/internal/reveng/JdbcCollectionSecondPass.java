package org.hibernate.tool.internal.reveng;

import java.lang.reflect.Field;
import java.util.Map;

import org.hibernate.MappingException;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.cfg.CollectionSecondPass;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.DependantValue;
import org.hibernate.mapping.OneToMany;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Value;

@SuppressWarnings("serial")
public class JdbcCollectionSecondPass extends CollectionSecondPass {

	MetadataBuildingContext mdbc;

    public JdbcCollectionSecondPass(MetadataBuildingContext mdbc, Collection coll) {
        super(mdbc, coll);
        this.mdbc = mdbc;
    }

   @SuppressWarnings("rawtypes")
   public void secondPass(Map persistentClasses, Map inheritedMetas) throws MappingException {
        bindCollectionSecondPass(getCollection(), persistentClasses, mdbc, inheritedMetas);
    }

    @SuppressWarnings("rawtypes")
	public void doSecondPass(Map persistentClasses) throws MappingException {
    	Value element = getCollection().getElement();
    	DependantValue elementDependantValue = null;
    	String oldElementForeignKeyName = null;
    	if(element instanceof DependantValue) {
			elementDependantValue = (DependantValue)element;
			oldElementForeignKeyName = elementDependantValue.getForeignKeyName();
    		elementDependantValue.setForeignKeyName("none"); // Workaround to avoid DependantValue to create foreignkey just because reference columns are not the same + no need to create keys already in the db!
    	}
    	Value key = getCollection().getKey();
    	DependantValue keyDependantValue = null;
    	String oldKeyForeignKeyName = null;
    	if (key instanceof DependantValue) {
    		keyDependantValue = (DependantValue)key;
    		oldKeyForeignKeyName = keyDependantValue.getForeignKeyName();
    		keyDependantValue.setForeignKeyName("none");
    	}
    	super.doSecondPass(persistentClasses);
    	if(elementDependantValue!=null) {
    		elementDependantValue.setForeignKeyName(oldElementForeignKeyName);
    	}
    	if (keyDependantValue != null) {
    		keyDependantValue.setForeignKeyName(oldKeyForeignKeyName);
    	}
    }

    private void bindCollectionSecondPass(
            Collection collection,
            Map<?,?> persistentClasses,
            MetadataBuildingContext mdbc,
            Map<?,?> inheritedMetas) throws MappingException {
        if(collection.isOneToMany() ) {
            OneToMany oneToMany = (OneToMany) collection.getElement();
            PersistentClass persistentClass = mdbc.getMetadataCollector().getEntityBinding(oneToMany.getReferencedEntityName());

            if (persistentClass==null) throw new MappingException(
                    "Association " + collection.getRole() + " references unmapped class: " + oneToMany.getReferencedEntityName()
                );

            oneToMany.setAssociatedClass(persistentClass); // Child
        }
    }
    
    private Collection getCollection() {
    	try {
    		Field field = getClass().getSuperclass().getDeclaredField("collection");
    		field.setAccessible(true);
    		return (Collection)field.get(this);
    	} catch (NoSuchFieldException e) {
    		// this will happen if the implementation of the superclass changes
    		throw new RuntimeException(e);
    	} catch (IllegalAccessException e) {
    		// this should not happen
    		throw new RuntimeException(e);
    	}
    }
    
}
    

package org.hibernate.tool.internal.reveng.binder;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.hibernate.FetchMode;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.DependantValue;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.OneToMany;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Table;
import org.hibernate.tool.api.reveng.TableIdentifier;

class OneToManyBinder extends AbstractBinder {
	
	static OneToManyBinder create(BinderContext binderContext) {
		return new OneToManyBinder(binderContext);
	}
	
	private final CollectionPropertyBinder collectionPropertyBinder;
	
	private OneToManyBinder(BinderContext binderContext) {
		super(binderContext);
		this.collectionPropertyBinder = CollectionPropertyBinder.create(binderContext);
	}

	Property bind(PersistentClass rc, ForeignKey foreignKey) {
		Collection collection = bindCollection(rc, foreignKey);
		getMetadataCollector().addCollectionBinding(collection);
		return collectionPropertyBinder
				.bind(
						StringHelper.unqualify(collection.getRole()), 
						true, 
						rc.getTable(), 
						foreignKey, 
						collection, 
						true);
	}
	
	private Collection bindCollection(PersistentClass pc, ForeignKey foreignKey) {
		Table table = foreignKey.getTable();
		Collection collection = new org.hibernate.mapping.Set(getMetadataBuildingContext(), pc); 
		collection.setCollectionTable(table); 
		boolean manyToMany = getRevengStrategy().isManyToManyTable( table );		
		if(manyToMany) {
        	bindManyToMany(pc, foreignKey, collection);
        } else {       	
        	bindOneToMany(pc, foreignKey, collection);
        }
		collection.setKey(createKeyValue(table, foreignKey, getReferencedKeyValue(collection)));
		return collection;
	}
		
	private KeyValue createKeyValue(Table collectionTable, ForeignKey foreignKey, KeyValue referencedKeyValue) {
		SimpleValue keyValue = new DependantValue(getMetadataBuildingContext(), collectionTable, referencedKeyValue);
		//keyValue.setForeignKeyName("none"); // Avoid creating the foreignkey
		//key.setCascadeDeleteEnabled( "cascade".equals( subnode.attributeValue("on-delete") ) );
		for (Column fkcolumn : foreignKey.getColumns()) {
			if(fkcolumn.getSqlTypeCode()!=null) { // TODO: user defined foreign ref columns does not have a type set.
				TypeUtils.determinePreferredType(
						getMetadataCollector(), 
						getRevengStrategy(),
						collectionTable, 
						fkcolumn, 
						false); // needed to ensure foreign key columns has same type as the "property" column.
			}
			keyValue.addColumn( fkcolumn );
		}
		return keyValue;
	}
	
	private KeyValue getReferencedKeyValue(Collection collection) {
		String propRef = collection.getReferencedPropertyName();
		if (propRef==null) {
			return collection.getOwner().getIdentifier();
		}
		else {
			return (KeyValue)collection.getOwner().getProperty(propRef).getValue();
		}
	}
	
	private void bindManyToMany(PersistentClass pc, ForeignKey fromForeignKey, Collection collection) {
    	ForeignKey toForeignKey = getToForeignKey(fromForeignKey);
    	bindCollection( pc, fromForeignKey, toForeignKey, collection );
    	ManyToOne manyToOne = new ManyToOne(getMetadataBuildingContext(), collection.getCollectionTable());
		manyToOne.setReferencedEntityName(getTableToClassName(toForeignKey.getReferencedTable()));	
		addColumns(manyToOne, toForeignKey);
		collection.setElement(manyToOne);
	}
	
	private void addColumns(ManyToOne manyToOne, ForeignKey fk) {
		Iterator<Column> columnIterator = fk.getColumns().iterator();
		while (columnIterator.hasNext()) {
			Column fkcolumn = (Column) columnIterator.next();
			if(fkcolumn.getSqlTypeCode() != null) {  // TODO: user defined foreign ref columns does not have a type set.
				TypeUtils.determinePreferredType(
						getMetadataCollector(), 
						getRevengStrategy(), 
						fk.getTable(), 
						fkcolumn, 
						false); // needed to ensure foreign key columns has same type as the "property" column.
			}
			manyToOne.addColumn(fkcolumn);
		}
	}
	
	private ForeignKey getToForeignKey(ForeignKey fromForeignKey) {
    	List<ForeignKey> keys = new ArrayList<ForeignKey>();
		for (ForeignKey foreignKey : fromForeignKey.getTable().getForeignKeys().values()) {
			if(foreignKey!=fromForeignKey) {
				keys.add(foreignKey);
			}
		}
    	if(keys.size()>1) {
    		throw new RuntimeException("more than one other foreign key to choose from!"); // todo: handle better ?
    	}
    	return (ForeignKey) keys.get( 0 );		
	}
	
	private void bindOneToMany(PersistentClass pc, ForeignKey fromForeignKey, Collection collection) {
    	bindCollection(pc, fromForeignKey, null, collection);
    	OneToMany oneToMany = new OneToMany(getMetadataBuildingContext(), collection.getOwner());
		oneToMany.setReferencedEntityName(getTableToClassName(fromForeignKey.getTable())); 
    	getMetadataCollector().addSecondPass(
    			new CollectionSecondPass(getMetadataBuildingContext(), collection));
    	collection.setElement(oneToMany);
	}


	private void bindCollection(PersistentClass pc, ForeignKey fromForeignKey, ForeignKey toForeignKey, Collection collection) {
		ForeignKey targetKey = toForeignKey != null ? toForeignKey : fromForeignKey;
		collection.setRole(getFullRolePath(pc, fromForeignKey, toForeignKey));  
		collection.setInverse(isCollectionInverse(targetKey)); 
		collection.setLazy(isCollectionLazy(targetKey));
		collection.setFetchMode(FetchMode.SELECT);
	}
	
	private boolean isCollectionLazy(ForeignKey foreignKey) {
		return getRevengStrategy().isForeignKeyCollectionLazy(
				foreignKey.getName(),
				TableIdentifier.create( foreignKey.getTable()),
				foreignKey.getColumns(),
				TableIdentifier.create(foreignKey.getReferencedTable()),
				foreignKey.getReferencedColumns());
	}	
	
	private boolean isCollectionInverse(ForeignKey foreignKey) {
		return getRevengStrategy().isForeignKeyCollectionInverse(
				foreignKey.getName(),
				foreignKey.getTable(),
				foreignKey.getColumns(),
				foreignKey.getReferencedTable(),
				foreignKey.getReferencedColumns());
	}
	
	private String getTableToClassName(Table table) {
		return getRevengStrategy().tableToClassName(TableIdentifier.create(table));
	}
	
	private String getFullRolePath(
			PersistentClass pc, 
			ForeignKey fromForeignKey, 
			ForeignKey toForeignKey) {
		String collectionRole = null;
		if(toForeignKey==null) {
			collectionRole = getForeignKeyToCollectionName(fromForeignKey);
		} else {
			collectionRole = getForeignKeyToManyToManyName(fromForeignKey, toForeignKey);
		}
		collectionRole = BinderUtils.makeUnique(pc,collectionRole);
		return StringHelper.qualify(pc.getEntityName(), collectionRole);
	}
	
	private String getForeignKeyToCollectionName(ForeignKey foreignKey) {
		return getRevengStrategy().foreignKeyToCollectionName(
				foreignKey.getName(),
				TableIdentifier.create(foreignKey.getTable()),
				foreignKey.getColumns(),
				TableIdentifier.create( foreignKey.getReferencedTable()),
				foreignKey.getReferencedColumns(),
				ForeignKeyUtils.isUniqueReference(foreignKey));
	}
	
	private String getForeignKeyToManyToManyName(ForeignKey fromForeignKey, ForeignKey toForeignKey) {
		return getRevengStrategy().foreignKeyToManyToManyName(
				fromForeignKey, 
				TableIdentifier.create(fromForeignKey.getTable()), 
				toForeignKey, 
				ForeignKeyUtils.isUniqueReference(toForeignKey));
	}
	
}

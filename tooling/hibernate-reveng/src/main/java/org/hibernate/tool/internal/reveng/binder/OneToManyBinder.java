package org.hibernate.tool.internal.reveng.binder;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.hibernate.FetchMode;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.engine.spi.Mapping;
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
import org.hibernate.tool.api.reveng.ReverseEngineeringStrategy;
import org.hibernate.tool.api.reveng.TableIdentifier;
import org.hibernate.tool.internal.reveng.JdbcCollectionSecondPass;

public class OneToManyBinder {
	
	public static OneToManyBinder create(BinderContext binderContext) {
		return new OneToManyBinder(binderContext);
	}
	
	private final MetadataBuildingContext metadataBuildingContext;
	private final InFlightMetadataCollector metadataCollector;
	private final ReverseEngineeringStrategy revengStrategy;
	private final CollectionPropertyBinder collectionPropertyBinder;
	
	private OneToManyBinder(BinderContext binderContext) {
		this.metadataBuildingContext = binderContext.metadataBuildingContext;
		this.metadataCollector = binderContext.metadataCollector;
		this.revengStrategy = binderContext.revengStrategy;
		this.collectionPropertyBinder = CollectionPropertyBinder.create(binderContext);
	}

	public Property bind(
			PersistentClass rc, 
			ForeignKey foreignKey, 
			Set<Column> processed, 
			Mapping mapping) {

		Table collectionTable = foreignKey.getTable();

		Collection collection = new org.hibernate.mapping.Set(metadataBuildingContext, rc); // MASTER TODO: allow overriding collection type

		collection.setCollectionTable(collectionTable); // CHILD+



		boolean manyToMany = revengStrategy.isManyToManyTable( collectionTable );
		if(manyToMany) {
			//log.debug("Rev.eng said here is a many-to-many");
			// TODO: handle "the other side should influence the name"
		}



        if(manyToMany) {

        	ManyToOne element = new ManyToOne(metadataBuildingContext, collection.getCollectionTable() );
        	//TODO: find the other foreignkey and choose the other side.
        	Iterator<?> foreignKeyIterator = foreignKey.getTable().getForeignKeyIterator();
        	List<ForeignKey> keys = new ArrayList<ForeignKey>();
        	while ( foreignKeyIterator.hasNext() ) {
				ForeignKey next = (ForeignKey)foreignKeyIterator.next();
				if(next!=foreignKey) {
					keys.add(next);
				}
			}

        	if(keys.size()>1) {
        		throw new RuntimeException("more than one other foreign key to choose from!"); // todo: handle better ?
        	}

        	ForeignKey fk = (ForeignKey) keys.get( 0 );

        	String tableToClassName = bindCollection( rc, foreignKey, fk, collection );

			element.setReferencedEntityName( tableToClassName );
			
			Iterator<Column> columnIterator = fk.getColumns().iterator();
			while (columnIterator.hasNext()) {
				Column fkcolumn = (Column) columnIterator.next();
				if(fkcolumn.getSqlTypeCode() != null) {  // TODO: user defined foreign ref columns does not have a type set.
					TypeUtils.determinePreferredType(
							metadataCollector, 
							revengStrategy, 
							fk.getTable(), 
							fkcolumn, 
							mapping, 
							false); // needed to ensure foreign key columns has same type as the "property" column.
				}
				element.addColumn(fkcolumn);
			}
			collection.setElement( element );

        } else {
        	String tableToClassName = bindCollection( rc, foreignKey, null, collection );

        	OneToMany oneToMany = new OneToMany(metadataBuildingContext, collection.getOwner() );

			oneToMany.setReferencedEntityName( tableToClassName ); // Child
        	metadataCollector.addSecondPass( new JdbcCollectionSecondPass(metadataBuildingContext, collection) );

        	collection.setElement(oneToMany);
        }
		// bind keyvalue
		KeyValue referencedKeyValue;
		String propRef = collection.getReferencedPropertyName();
		if (propRef==null) {
			referencedKeyValue = collection.getOwner().getIdentifier();
		}
		else {
			referencedKeyValue = (KeyValue) collection.getOwner()
				.getProperty(propRef)
				.getValue();
		}

		SimpleValue keyValue = new DependantValue(metadataBuildingContext, collectionTable, referencedKeyValue );
		//keyValue.setForeignKeyName("none"); // Avoid creating the foreignkey
		//key.setCascadeDeleteEnabled( "cascade".equals( subnode.attributeValue("on-delete") ) );
		Iterator<Column> columnIterator = foreignKey.getColumnIterator();
		while ( columnIterator.hasNext() ) {
			Column fkcolumn = columnIterator.next();
			if(fkcolumn.getSqlTypeCode()!=null) { // TODO: user defined foreign ref columns does not have a type set.
				TypeUtils.determinePreferredType(
						metadataCollector, 
						revengStrategy,
						collectionTable, 
						fkcolumn, 
						mapping, 
						false); // needed to ensure foreign key columns has same type as the "property" column.
			}
			keyValue.addColumn( fkcolumn );
		}

		collection.setKey(keyValue);

		metadataCollector.addCollectionBinding(collection);

		return collectionPropertyBinder
				.bind(
						StringHelper.unqualify( collection.getRole()), 
						true, rc.getTable(), 
						foreignKey, 
						collection, 
						true);


	}


	private String bindCollection(PersistentClass rc, ForeignKey fromForeignKey, ForeignKey toForeignKey, Collection collection) {
		ForeignKey targetKey = fromForeignKey;
		String collectionRole = null;
		boolean collectionLazy = false;
		boolean collectionInverse = false;
		TableIdentifier foreignKeyTable = null;
		String tableToClassName;

		if(toForeignKey!=null) {
			targetKey = toForeignKey;
		}

		boolean uniqueReference = ForeignKeyUtils.isUniqueReference(targetKey); // TODO: need to look one step further for many-to-many!
		foreignKeyTable = TableIdentifier.create( targetKey.getTable() );
		TableIdentifier foreignKeyReferencedTable = TableIdentifier.create( targetKey.getReferencedTable() );

		if(toForeignKey==null) {

			collectionRole = revengStrategy.foreignKeyToCollectionName(
				fromForeignKey.getName(),
				foreignKeyTable,
				fromForeignKey.getColumns(),
				foreignKeyReferencedTable,
				fromForeignKey.getReferencedColumns(),
				uniqueReference
			);

			tableToClassName = revengStrategy.tableToClassName( foreignKeyTable );
		} else {

			collectionRole = revengStrategy.foreignKeyToManyToManyName(
					fromForeignKey, TableIdentifier.create( fromForeignKey.getTable()), toForeignKey, uniqueReference );

			tableToClassName = revengStrategy.tableToClassName( foreignKeyReferencedTable );
		}

		collectionInverse = revengStrategy.isForeignKeyCollectionInverse(
			targetKey.getName(),
			foreignKeyTable,
			targetKey.getColumns(),
			foreignKeyReferencedTable,
			targetKey.getReferencedColumns());

		collectionLazy = revengStrategy.isForeignKeyCollectionLazy(
			targetKey.getName(),
			foreignKeyTable,
			targetKey.getColumns(),
			foreignKeyReferencedTable,
			targetKey.getReferencedColumns());

		collectionRole = BinderUtils.makeUnique(rc,collectionRole);

		String fullRolePath = StringHelper.qualify(rc.getEntityName(), collectionRole);
		collection.setRole(fullRolePath);  // Master.setOfChildren+
		collection.setInverse(collectionInverse); // TODO: allow overriding this
		collection.setLazy(collectionLazy);
		collection.setFetchMode(FetchMode.SELECT);


		return tableToClassName;
	}

}

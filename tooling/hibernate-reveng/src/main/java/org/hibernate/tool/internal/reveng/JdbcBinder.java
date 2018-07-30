/*
 * Created on 2004-11-23
 *
 */
package org.hibernate.tool.internal.reveng;


import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.hibernate.DuplicateMappingException;
import org.hibernate.FetchMode;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.internal.InFlightMetadataCollectorImpl;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.DependantValue;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.MetaAttribute;
import org.hibernate.mapping.OneToMany;
import org.hibernate.mapping.OneToOne;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.ToOne;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.api.reveng.AssociationInfo;
import org.hibernate.tool.api.reveng.DatabaseCollector;
import org.hibernate.tool.api.reveng.ReverseEngineeringStrategy;
import org.hibernate.tool.api.reveng.TableIdentifier;
import org.hibernate.tool.internal.util.JdbcToHibernateTypeHelper;
import org.hibernate.tool.internal.util.TableNameQualifier;
import org.hibernate.type.ForeignKeyDirection;
import org.hibernate.type.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author max
 *
 */
public class JdbcBinder {

	private Properties properties;
	private static final Logger log = LoggerFactory.getLogger(JdbcBinder.class);

	private final MetadataBuildingContext mdbc;
	
	private final InFlightMetadataCollector metadataCollector;
	
	private Metadata metadata;

	private ReverseEngineeringStrategy revengStrategy;
	
	private final boolean preferBasicCompositeIds;
	private final ServiceRegistry serviceRegistry;
	private final String defaultCatalog;
	private final String defaultSchema;

	/**
	 * @param mappings
	 * @param configuration
	 */
	public JdbcBinder(ServiceRegistry serviceRegistry, Properties properties, MetadataBuildingContext mdbc, ReverseEngineeringStrategy revengStrategy, boolean preferBasicCompositeIds) {
		this.serviceRegistry = serviceRegistry;
		this.mdbc = mdbc;
		this.properties = properties;
		this.revengStrategy = revengStrategy;
		this.preferBasicCompositeIds = preferBasicCompositeIds;
		this.defaultCatalog = properties.getProperty(AvailableSettings.DEFAULT_CATALOG);
		this.defaultSchema = properties.getProperty(AvailableSettings.DEFAULT_SCHEMA);
		metadataCollector = mdbc.getMetadataCollector();
		this.metadata = ((InFlightMetadataCollectorImpl)metadataCollector).buildMetadataInstance(mdbc);
	}

	/**
	 *
	 */
	public void readFromDatabase(String catalog, String schema, Mapping mapping) {
		try {
			DatabaseCollector collector = readDatabaseSchema(catalog, schema);
			createPersistentClasses(collector, mapping); //move this to a different step!
			((InFlightMetadataCollectorImpl)metadataCollector).processSecondPasses(mdbc);
		}
		catch (SQLException e) {
			JdbcServices jdbcServices = serviceRegistry.getService(JdbcServices.class);
			throw jdbcServices.getSqlExceptionHelper().convert(e, "Reading from database", null);
		}
	}

	/**
	 * Read JDBC Metadata from the database. Does not create any classes or other ORM releated structures.
	 *
	 * @param catalog
	 * @param schema
	 * @return
	 * @throws SQLException
	 */
	public DatabaseCollector readDatabaseSchema(String catalog, String schema) throws SQLException {
	  	 // use default from settings if nothing else specified.
	     catalog = catalog!=null ? catalog : properties.getProperty(AvailableSettings.DEFAULT_CATALOG);
	     schema = schema!=null ? schema : properties.getProperty(AvailableSettings.DEFAULT_SCHEMA);

	     JDBCReader reader = JdbcReaderFactory.newJDBCReader(properties,revengStrategy,serviceRegistry);
	     DatabaseCollector dbs = new MappingsDatabaseCollector(metadataCollector, reader.getMetaDataDialect());

	     reader.readDatabaseSchema(dbs, catalog, schema);
	     return dbs;
	}



	/**
	 * @param manyToOneCandidates
	 * @param mappings2
	 */
	private void createPersistentClasses(DatabaseCollector collector, Mapping mapping) {
		Map<String, List<ForeignKey>> manyToOneCandidates = collector.getOneToManyCandidates();
		for (Iterator<Table> iter = metadataCollector.collectTableMappings().iterator(); iter.hasNext();) {
			Table table = iter.next();
			if (table.getCatalog() != null && table.getCatalog().equals(defaultCatalog)) {
				table.setCatalog(null);
			}
			if (table.getSchema() != null && table.getSchema().equals(defaultSchema)) {
				table.setSchema(null);
			}
			if(table.getColumnSpan()==0) {
				log.warn("Cannot create persistent class for " + table + " as no columns were found.");
				continue;
			}
			// TODO: this naively just create an entity per table
			// should have an opt-out option to mark some as helper tables, subclasses etc.
			/*if(table.getPrimaryKey()==null || table.getPrimaryKey().getColumnSpan()==0) {
			    log.warn("Cannot create persistent class for " + table + " as no primary key was found.");
                continue;
                // TODO: just create one big embedded composite id instead.
            }*/

			if(revengStrategy.isManyToManyTable(table)) {
				log.debug( "Ignoring " + table + " as class since rev.eng. says it is a many-to-many" );
				continue;
			}

	    	
			RootClass rc = new RootClass(mdbc);
			TableIdentifier tableIdentifier = TableIdentifier.create(table);
			String className = revengStrategy.tableToClassName( tableIdentifier );
			log.debug("Building entity " + className + " based on " + tableIdentifier);
			rc.setEntityName( className );
			rc.setJpaEntityName( StringHelper.unqualify( className ) );
			rc.setClassName( className );
			rc.setProxyInterfaceName( rc.getEntityName() ); // TODO: configurable ?
			rc.setLazy(true);

			rc.setMetaAttributes(
					BinderUtils.safeMap(
							RevEngUtils.getTableToMetaAttributesInRevengStrategy(
									revengStrategy, 
									table, 
									defaultCatalog, 
									defaultSchema)));


			rc.setDiscriminatorValue( rc.getEntityName() );
			rc.setTable(table);
			try {
				metadataCollector.addEntityBinding(rc);
			} catch(DuplicateMappingException dme) {
				// TODO: detect this and generate a "permutation" of it ?
				PersistentClass class1 = metadataCollector.getEntityBinding(dme.getName());
				Table table2 = class1.getTable();
				throw new JdbcBinderException("Duplicate class name '" + rc.getEntityName() + "' generated for '" + table + "'. Same name where generated for '" + table2 + "'");
			}
			metadataCollector.addImport( rc.getEntityName(), rc.getEntityName() );

			Set<Column> processed = new HashSet<Column>();


			PrimaryKeyInfo pki = bindPrimaryKeyToProperties(table, rc, processed, mapping, collector);
			bindColumnsToVersioning(table, rc, processed, mapping);
			bindOutgoingForeignKeys(table, rc, processed);
			bindColumnsToProperties(table, rc, processed, mapping);
			List<ForeignKey> incomingForeignKeys = manyToOneCandidates.get( rc.getEntityName() );
			bindIncomingForeignKeys(rc, processed, incomingForeignKeys, mapping);
			updatePrimaryKey(rc, pki);

		}

	}

	private void updatePrimaryKey(RootClass rc, PrimaryKeyInfo pki) {
		SimpleValue idValue = (SimpleValue) rc.getIdentifierProperty().getValue();

		Properties defaultStrategyProperties = new Properties();
		Property constrainedOneToOne = getConstrainedOneToOne(rc);
		if(constrainedOneToOne!=null) {
			if(pki.suggestedStrategy==null) {
				idValue.setIdentifierGeneratorStrategy("foreign");
			}

			if(pki.suggestedProperties==null) {
				defaultStrategyProperties.setProperty("property", constrainedOneToOne.getName());
				idValue.setIdentifierGeneratorProperties(defaultStrategyProperties);
			}
		}



	}

	private Property getConstrainedOneToOne(RootClass rc) {
		Iterator<?> propertyClosureIterator = rc.getPropertyClosureIterator();
		while (propertyClosureIterator.hasNext()) {
			Property property = (Property) propertyClosureIterator.next();
			if(property.getValue() instanceof OneToOne) {
				OneToOne oto = (OneToOne) property.getValue();
				if(oto.isConstrained()) {
					return property;
				}
			}
		}
		return null;
	}

	// bind collections.
	@SuppressWarnings("unchecked")
	private void bindIncomingForeignKeys(PersistentClass rc, Set<Column> processed, List<ForeignKey> foreignKeys, Mapping mapping) {
		if(foreignKeys!=null) {
			for (Iterator<ForeignKey> iter = foreignKeys.iterator(); iter.hasNext();) {
				ForeignKey foreignKey = iter.next();

				if(revengStrategy.excludeForeignKeyAsCollection(
						foreignKey.getName(),
						TableIdentifier.create(foreignKey.getTable() ),
						foreignKey.getColumns(),
						TableIdentifier.create(foreignKey.getReferencedTable() ),
						foreignKey.getReferencedColumns())) {
					log.debug("Rev.eng excluded one-to-many or one-to-one for foreignkey " + foreignKey.getName());
				} else if (revengStrategy.isOneToOne(foreignKey)){
					Property property = bindOneToOne(rc, foreignKey.getTable(), foreignKey, processed, false, true);
					rc.addProperty(property);
				} else {
					Property property = bindOneToMany(rc, foreignKey, processed, mapping);
					rc.addProperty(property);
				}
			}
		}
	}


    private Property bindOneToOne(PersistentClass rc, Table targetTable,
            ForeignKey fk, Set<Column> processedColumns, boolean constrained, boolean inverseProperty) {

        OneToOne value = new OneToOne(mdbc, targetTable, rc);
        value.setReferencedEntityName(revengStrategy
                .tableToClassName(TableIdentifier.create(targetTable)));

        boolean isUnique = isUniqueReference(fk);
        String propertyName = null;
        if(inverseProperty) {
            propertyName = revengStrategy.foreignKeyToInverseEntityName(
        		fk.getName(),
                TableIdentifier.create(fk.getReferencedTable()), 
                fk.getReferencedColumns(), 
                TableIdentifier.create(targetTable), 
                fk.getColumns(), 
                isUnique);
        } else {
            propertyName = revengStrategy.foreignKeyToEntityName(
        		fk.getName(),
                TableIdentifier.create(fk.getReferencedTable()), 
                fk.getReferencedColumns(), 
                TableIdentifier.create(targetTable), 
                fk.getColumns(), 
                isUnique);
        }

        Iterator<Column> columns = fk.getColumnIterator();
        while (columns.hasNext()) {
            Column fkcolumn = (Column) columns.next();
            checkColumn(fkcolumn);
            value.addColumn(fkcolumn);
            processedColumns.add(fkcolumn);
        }

        value.setFetchMode(FetchMode.SELECT);

        value.setConstrained(constrained);
        value.setForeignKeyType( constrained ?
				ForeignKeyDirection.FROM_PARENT :
				ForeignKeyDirection.TO_PARENT );


        return makeEntityProperty(propertyName, true, targetTable, fk, value, inverseProperty);
        //return makeProperty(TableIdentifier.create(targetTable), propertyName, value,
        //        true, true, value.getFetchMode() != FetchMode.JOIN, null, null);
    }

    /**
     * @param mutable
     * @param table
     * @param fk
     * @param columnsToBind
     * @param processedColumns
     * @param rc
     * @param propName
     */
    private Property bindManyToOne(String propertyName, boolean mutable, Table table, ForeignKey fk, Set<Column> processedColumns) {
        ManyToOne value = new ManyToOne(mdbc, table);
        value.setReferencedEntityName( fk.getReferencedEntityName() );
		Iterator<Column> columns = fk.getColumnIterator();
        while ( columns.hasNext() ) {
			Column fkcolumn = (Column) columns.next();
            checkColumn(fkcolumn);
            value.addColumn(fkcolumn);
            processedColumns.add(fkcolumn);
		}
        value.setFetchMode(FetchMode.SELECT);

        return makeEntityProperty(propertyName, mutable, table, fk, value, false);
     }

    private Property makeCollectionProperty(String propertyName, boolean mutable,
			Table table, ForeignKey fk, Collection value, boolean inverseProperty) {
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

        return PropertyBinder.makeProperty(
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

	private Property makeEntityProperty(String propertyName, boolean mutable,
			Table table, ForeignKey fk, ToOne value, boolean inverseProperty) {
		AssociationInfo fkei = inverseProperty?revengStrategy.foreignKeyToInverseAssociationInfo(fk):revengStrategy.foreignKeyToAssociationInfo(fk);

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

        return PropertyBinder.makeProperty(
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

	/**
	 * @param rc
	 * @param processed
	 * @param table
	 * @param object
	 */
	private Property bindOneToMany(PersistentClass rc, ForeignKey foreignKey, Set<Column> processed, Mapping mapping) {

		Table collectionTable = foreignKey.getTable();

		Collection collection = new org.hibernate.mapping.Set(mdbc, rc); // MASTER TODO: allow overriding collection type

		collection.setCollectionTable(collectionTable); // CHILD+



		boolean manyToMany = revengStrategy.isManyToManyTable( collectionTable );
		if(manyToMany) {
			//log.debug("Rev.eng said here is a many-to-many");
			// TODO: handle "the other side should influence the name"
		}



        if(manyToMany) {

        	ManyToOne element = new ManyToOne(mdbc, collection.getCollectionTable() );
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
        		throw new JdbcBinderException("more than one other foreign key to choose from!"); // todo: handle better ?
        	}

        	ForeignKey fk = (ForeignKey) keys.get( 0 );

        	String tableToClassName = bindCollection( rc, foreignKey, fk, collection );

			element.setReferencedEntityName( tableToClassName );
			
			Iterator<Column> columnIterator = fk.getColumns().iterator();
			while (columnIterator.hasNext()) {
				Column fkcolumn = (Column) columnIterator.next();
				if(fkcolumn.getSqlTypeCode() != null) {  // TODO: user defined foreign ref columns does not have a type set.
					guessAndAlignType(fk.getTable(), fkcolumn, mapping, false); // needed to ensure foreign key columns has same type as the "property" column.
				}
				element.addColumn(fkcolumn);
			}
			collection.setElement( element );

        } else {
        	String tableToClassName = bindCollection( rc, foreignKey, null, collection );

        	OneToMany oneToMany = new OneToMany(mdbc, collection.getOwner() );

			oneToMany.setReferencedEntityName( tableToClassName ); // Child
        	metadataCollector.addSecondPass( new JdbcCollectionSecondPass(mdbc, collection) );

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

		SimpleValue keyValue = new DependantValue(mdbc, collectionTable, referencedKeyValue );
		//keyValue.setForeignKeyName("none"); // Avoid creating the foreignkey
		//key.setCascadeDeleteEnabled( "cascade".equals( subnode.attributeValue("on-delete") ) );
		Iterator<Column> columnIterator = foreignKey.getColumnIterator();
		while ( columnIterator.hasNext() ) {
			Column fkcolumn = columnIterator.next();
			if(fkcolumn.getSqlTypeCode()!=null) { // TODO: user defined foreign ref columns does not have a type set.
				guessAndAlignType(collectionTable, fkcolumn, mapping, false); // needed to ensure foreign key columns has same type as the "property" column.
			}
			keyValue.addColumn( fkcolumn );
		}

		collection.setKey(keyValue);

		metadataCollector.addCollectionBinding(collection);

		return makeCollectionProperty(StringHelper.unqualify( collection.getRole() ), true, rc.getTable(), foreignKey, collection, true);
		//return makeProperty(TableIdentifier.create( rc.getTable() ), StringHelper.unqualify( collection.getRole() ), collection, true, true, true, "none", null); // TODO: cascade isn't all by default


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

		boolean uniqueReference = isUniqueReference(targetKey); // TODO: need to look one step further for many-to-many!
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
		if (metadata.getCollectionBinding(fullRolePath)!=null) {
		    log.debug(fullRolePath + " found twice!");
		}

		collection.setRole(fullRolePath);  // Master.setOfChildren+
		collection.setInverse(collectionInverse); // TODO: allow overriding this
		collection.setLazy(collectionLazy);
		collection.setFetchMode(FetchMode.SELECT);


		return tableToClassName;
	}

	/** return true if this foreignkey is the only reference from this table to the same foreign table */
    private boolean isUniqueReference(ForeignKey foreignKey) {

    	Iterator<?> foreignKeyIterator = foreignKey.getTable().getForeignKeyIterator();
    	while ( foreignKeyIterator.hasNext() ) {
			ForeignKey element = (ForeignKey) foreignKeyIterator.next();
			if(element!=foreignKey && element.getReferencedTable().equals(foreignKey.getReferencedTable())) {
				return false;
			}
		}
		return true;
	}

	private PrimaryKeyInfo bindPrimaryKeyToProperties(Table table, RootClass rc, Set<Column> processed, Mapping mapping, DatabaseCollector collector) {
		SimpleValue id = null;
		String idPropertyname = null;

		PrimaryKeyInfo pki = new PrimaryKeyInfo();

		List<Column> keyColumns = null;
		if (table.getPrimaryKey()!=null) {
			keyColumns = table.getPrimaryKey().getColumns();
		}
		else {
			log.debug("No primary key found for " + table + ", using all properties as the identifier.");
			keyColumns = new ArrayList<Column>();
			Iterator<?> iter = table.getColumnIterator();
			while (iter.hasNext() ) {
				Column col = (Column) iter.next();
				keyColumns.add(col);
			}
		}

		final TableIdentifier tableIdentifier = TableIdentifier.create(table);

		String tableIdentifierStrategyName = "assigned";

		boolean naturalId;

		if (keyColumns.size()>1) {
			log.debug("id strategy for " + rc.getEntityName() + " since it has a multiple column primary key");
			naturalId = true;

			id = handleCompositeKey(rc, processed, keyColumns, mapping);
			idPropertyname = revengStrategy.tableToIdentifierPropertyName(tableIdentifier);
			if(idPropertyname==null) {
				idPropertyname = "id";
			}
		}
		else {
			pki.suggestedStrategy = RevEngUtils.getTableIdentifierStrategyNameInRevengStrategy(revengStrategy, table, defaultCatalog, defaultSchema);
			String suggestedStrategy = pki.suggestedStrategy;
			if(suggestedStrategy==null) {
				suggestedStrategy = collector.getSuggestedIdentifierStrategy( tableIdentifier.getCatalog(), tableIdentifier.getSchema(), tableIdentifier.getName() );
				if(suggestedStrategy==null) {
					suggestedStrategy = "assigned";
				}
				tableIdentifierStrategyName = suggestedStrategy;
			} else {
				tableIdentifierStrategyName = suggestedStrategy;
			}

			naturalId = "assigned".equals( tableIdentifierStrategyName );
			Column pkc = (Column) keyColumns.get(0);
			checkColumn(pkc);

			id = bindColumnToSimpleValue(table, pkc, mapping, !naturalId);

			idPropertyname = revengStrategy.tableToIdentifierPropertyName(tableIdentifier);
			if(idPropertyname==null) {
				idPropertyname = revengStrategy.columnToPropertyName(tableIdentifier, pkc.getName() );
			}

			processed.add(pkc);
		}
		id.setIdentifierGeneratorStrategy(tableIdentifierStrategyName);
		pki.suggestedProperties = revengStrategy.getTableIdentifierProperties(tableIdentifier);
		id.setIdentifierGeneratorProperties(pki.suggestedProperties);
		if(naturalId) {
			id.setNullValue("undefined");
		}

		Property property = PropertyBinder.makeProperty(
				table, 
				defaultCatalog,
				defaultSchema,
				BinderUtils.makeUnique(rc,idPropertyname), 
				id, 
				true, 
				true, 
				false, 
				null, 
				null,
				revengStrategy);
		rc.setIdentifierProperty(property);
		rc.setIdentifier(id);

		return pki;
	}

	/**
	 * bind many-to-ones
	 * @param table
	 * @param rc
	 * @param primaryKey
	 */
	private void bindOutgoingForeignKeys(Table table, RootClass rc, Set<Column> processedColumns) {

		// Iterate the outgoing foreign keys and create many-to-one's
		for(Iterator<?> iterator = table.getForeignKeyIterator(); iterator.hasNext();) {
			ForeignKey foreignKey = (ForeignKey) iterator.next();

			boolean mutable = true;
            if ( contains( foreignKey.getColumnIterator(), processedColumns ) ) {
				if ( !preferBasicCompositeIds ) continue; //it's in the pk, so skip this one
				mutable = false;
            }

            if(revengStrategy.excludeForeignKeyAsManytoOne(foreignKey.getName(),
        			TableIdentifier.create(foreignKey.getTable() ),
        			foreignKey.getColumns(),
        			TableIdentifier.create(foreignKey.getReferencedTable() ),
        			foreignKey.getReferencedColumns())) {
            	// TODO: if many-to-one is excluded should the column be marked as processed so it won't show up at all ?
            	log.debug("Rev.eng excluded *-to-one for foreignkey " + foreignKey.getName());
            } else if (revengStrategy.isOneToOne(foreignKey)){
				Property property = bindOneToOne(rc, foreignKey.getReferencedTable(), foreignKey, processedColumns, true, false);
				rc.addProperty(property);
			} else {
            	boolean isUnique = isUniqueReference(foreignKey);
            	String propertyName = revengStrategy.foreignKeyToEntityName(
            			foreignKey.getName(),
            			TableIdentifier.create(foreignKey.getTable() ),
            			foreignKey.getColumns(),
            			TableIdentifier.create(foreignKey.getReferencedTable() ),
            			foreignKey.getReferencedColumns(),
            			isUnique
            	);

            	Property property = bindManyToOne(
            			BinderUtils.makeUnique(rc, propertyName),
            			mutable,
            			table,
            			foreignKey,
            			processedColumns
            	);

            	rc.addProperty(property);
            }
		}
	}

	/**
	 * @param table
	 * @param rc
	 * @param primaryKey
	 */
	private void bindColumnsToProperties(Table table, RootClass rc, Set<Column> processedColumns, Mapping mapping) {

		for (Iterator<?> iterator = table.getColumnIterator(); iterator.hasNext();) {
			Column column = (Column) iterator.next();
			if ( !processedColumns.contains(column) ) {
				checkColumn(column);

				String propertyName = 
						RevEngUtils.getColumnToPropertyNameInRevengStrategy(
								revengStrategy, 
								table, 
								defaultCatalog, 
								defaultSchema, 
								column.getName());

				Property property = bindBasicProperty(
						BinderUtils.makeUnique(rc,propertyName),
						table,
						column,
						processedColumns,
						mapping
					);

				rc.addProperty(property);
			}
		}
	}

	private void bindColumnsToVersioning(Table table, RootClass rc, Set<Column> processed, Mapping mapping) {
		TableIdentifier identifier = TableIdentifier.create(table);

		String optimisticLockColumnName = revengStrategy.getOptimisticLockColumnName(identifier);

		if(optimisticLockColumnName!=null) {
			Column column = table.getColumn(new Column(optimisticLockColumnName));
			if(column==null) {
				log.warn("Column " + column + " wanted for <version>/<timestamp> not found in " + identifier);
			} else {
				bindVersionProperty(table, identifier, column, rc, processed, mapping);
			}
		} else {
			log.debug("Scanning " + identifier + " for <version>/<timestamp> columns.");
			Iterator<?> columnIterator = table.getColumnIterator();
			while(columnIterator.hasNext()) {
				Column column = (Column) columnIterator.next();
				boolean useIt = revengStrategy.useColumnForOptimisticLock(identifier, column.getName());
				if(useIt && !processed.contains(column)) {
					bindVersionProperty( table, identifier, column, rc, processed, mapping );
					return;
				}
			}
			log.debug("No columns reported while scanning for <version>/<timestamp> columns in " + identifier);
		}
	}

	private void bindVersionProperty(Table table, TableIdentifier identifier, Column column, RootClass rc, Set<Column> processed, Mapping mapping) {

		processed.add(column);
		String propertyName = revengStrategy.columnToPropertyName( identifier, column.getName() );
		Property property = bindBasicProperty(BinderUtils.makeUnique(rc, propertyName), table, column, processed, mapping);
		rc.addProperty(property);
		rc.setVersion(property);
		rc.setOptimisticLockStyle(OptimisticLockStyle.VERSION);
		log.debug("Column " + column.getName() + " will be used for <version>/<timestamp> columns in " + identifier);

	}

	private Property bindBasicProperty(String propertyName, Table table, Column column, Set<Column> processedColumns, Mapping mapping) {
		SimpleValue value = bindColumnToSimpleValue( table, column, mapping, false );
		return PropertyBinder.makeProperty(
				table, 
				defaultCatalog,
				defaultSchema,
				propertyName, 
				value, 
				true, 
				true, 
				false, 
				null, 
				null,
				revengStrategy);
	}

	private SimpleValue bindColumnToSimpleValue(Table table, Column column, Mapping mapping, boolean generatedIdentifier) {
		SimpleValue value = new SimpleValue(mdbc, table);
		value.addColumn(column);
		value.setTypeName(guessAndAlignType(table, column, mapping, generatedIdentifier));
		return value;
	}

    /**
     * @param columnIterator
     * @param processedColumns
     * @return
     */
    private boolean contains(Iterator<Column> columnIterator, Set<Column> processedColumns) {
        while (columnIterator.hasNext() ) {
            Column element = (Column) columnIterator.next();
            if(processedColumns.contains(element) ) {
                return true;
            }
        }
        return false;
    }

	private void checkColumn(Column column) {
		if(column.getValue()!=null) {
			//throw new JDBCBinderException("Binding column twice should not happen. " + column);
		}
	}

	/**
	 * @param column
	 * @param generatedIdentifier
	 * @return
	 */
	private String guessAndAlignType(Table table, Column column, Mapping mapping, boolean generatedIdentifier) {
		// TODO: this method mutates the column if the types does not match...not good.
		// maybe we should copy the column instead before calling this method.
		Integer sqlTypeCode = column.getSqlTypeCode();
		String location = 
				"Table: " + 
				TableNameQualifier.qualify(table.getCatalog(), table.getSchema(), table.getQuotedName() ) + 
				" column: " + 
				column.getQuotedName();
		if(sqlTypeCode==null) {
			throw new JdbcBinderException("sqltype is null for " + location);
		}

		String preferredHibernateType = revengStrategy.columnToHibernateTypeName(
				TableIdentifier.create(table),
				column.getName(),
				sqlTypeCode.intValue(),
				column.getLength(), column.getPrecision(), column.getScale(), column.isNullable(), generatedIdentifier
		);

		Type wantedType = metadataCollector.getTypeResolver().heuristicType(preferredHibernateType);

		if(wantedType!=null) {
			int[] wantedSqlTypes = wantedType.sqlTypes(mapping);

			if(wantedSqlTypes.length>1) {
				throw new JdbcBinderException("The type " + preferredHibernateType + " found on " + location + " spans multiple columns. Only single column types allowed.");
			}

			int wantedSqlType = wantedSqlTypes[0];
			if(wantedSqlType!=sqlTypeCode.intValue() ) {
				log.debug("Sql type mismatch for " + location + " between DB and wanted hibernate type. Sql type set to " + typeCodeName( sqlTypeCode.intValue() ) + " instead of " + typeCodeName(wantedSqlType) );
				column.setSqlTypeCode(new Integer(wantedSqlType));
			}
		}
		else {
			log.debug("No Hibernate type found for " + preferredHibernateType + ". Most likely cause is a missing UserType class.");
		}



		if(preferredHibernateType==null) {
			throw new JdbcBinderException("Could not find javatype for " + typeCodeName(sqlTypeCode.intValue()));
		}

		return preferredHibernateType;
	}

	private String typeCodeName(int sqlTypeCode) {
		return sqlTypeCode + "(" + JdbcToHibernateTypeHelper.getJDBCTypeName(sqlTypeCode) + ")";
	}

	/**
     * Basically create an [classname]Id.class and add  properties for it.
	 * @param rc
	 * @param compositeKeyColumns
	 * @param processed
	 * @return
	 */
	private SimpleValue handleCompositeKey(RootClass rc, Set<Column> processedColumns, List<Column> keyColumns, Mapping mapping) {
		Component pkc = new Component(mdbc, rc);
        pkc.setMetaAttributes(Collections.EMPTY_MAP);
        pkc.setEmbedded(false);

        String compositeIdName = revengStrategy.tableToCompositeIdName(TableIdentifier.create(rc.getTable()));
        if(compositeIdName==null) {
        	compositeIdName = revengStrategy.classNameToCompositeIdName(rc.getClassName());
        }
        pkc.setComponentClassName(compositeIdName);
		Table table = rc.getTable();
        List<Object> list = null;
		if (preferBasicCompositeIds ) {
            list = new ArrayList<Object>(keyColumns);
        }
		else {
            list = findForeignKeys(table.getForeignKeyIterator(), keyColumns);
        }
        for (Iterator<?> iter = list.iterator(); iter.hasNext();) {
            Object element = iter.next();
			Property property;
            if (element instanceof Column) {
                Column column = (Column) element;
                if ( processedColumns.contains(column) ) {
                    throw new JdbcBinderException("Binding column twice for primary key should not happen: " + column);
                }
				else {
                    checkColumn(column);

                    String propertyName = revengStrategy.columnToPropertyName( TableIdentifier.create(table), column.getName() );
					property = bindBasicProperty( BinderUtils.makeUnique(pkc, propertyName), table, column, processedColumns, mapping);

                    processedColumns.add(column);
                }
            }
			else if (element instanceof ForeignKeyForColumns) {
                ForeignKeyForColumns fkfc = (ForeignKeyForColumns) element;
                ForeignKey foreignKey = fkfc.key;
                String propertyName = revengStrategy.foreignKeyToEntityName(
						foreignKey.getName(),
						TableIdentifier.create(foreignKey.getTable() ),
						foreignKey.getColumns(), TableIdentifier.create(foreignKey.getReferencedTable() ), foreignKey.getReferencedColumns(), true
					);
                property = bindManyToOne( BinderUtils.makeUnique(pkc, propertyName), true, table, foreignKey, processedColumns);
                processedColumns.addAll(fkfc.columns);
            }
			else {
				throw new JdbcBinderException("unknown thing");
			}

            markAsUseInEquals(property);
            pkc.addProperty(property);

		}

		return pkc;
	}

    /**
     * @param property
     */
    private void markAsUseInEquals(Property property) {
        Map<String, MetaAttribute> m = new HashMap<String, MetaAttribute>();
        MetaAttribute ma = new MetaAttribute("use-in-equals");
        ma.addValue("true");
        m.put(ma.getName(),ma);
        property.setMetaAttributes(m);
    }

    /**
     * @param foreignKeyIterator
     * @param columns
     * @return
     */
    private List<Object> findForeignKeys(Iterator<?> foreignKeyIterator, List<Column> pkColumns) {

    	List<ForeignKey> tempList = new ArrayList<ForeignKey>();
    	while(foreignKeyIterator.hasNext()) {
    		tempList.add((ForeignKey)foreignKeyIterator.next());
    	}

//    	Collections.reverse(tempList);

    	List<Object> result = new ArrayList<Object>();
    	Column myPkColumns[] = (Column[]) pkColumns.toArray(new Column[pkColumns.size()]);

    	for (int i = 0; i < myPkColumns.length; i++) {

    		boolean foundKey = false;
    		foreignKeyIterator = tempList.iterator();
    		while(foreignKeyIterator.hasNext()) {
    			ForeignKey key = (ForeignKey) foreignKeyIterator.next();
    			List<Column> matchingColumns = columnMatches(myPkColumns, i, key);
    			if(matchingColumns!=null) {
    				result.add(new ForeignKeyForColumns(key, matchingColumns));
    				i+=matchingColumns.size()-1;
    				foreignKeyIterator.remove();
    				foundKey=true;
    				break;
    			}
    		}
    		if(!foundKey) {
    			result.add(myPkColumns[i]);
    		}

		}

    	return result;
    }

    private List<Column> columnMatches(Column[] myPkColumns, int offset, ForeignKey key) {

    	if(key.getColumnSpan()>(myPkColumns.length-offset)) {
    		return null; // not enough columns in the key
    	}

    	List<Column> columns = new ArrayList<Column>();
    	for (int j = 0; j < key.getColumnSpan(); j++) {
			Column column = myPkColumns[j+offset];
			if(!column.equals(key.getColumn(j))) {
				return null;
			} else {
				columns.add(column);
			}
		}
		return columns.isEmpty()?null:columns;
	}

	static class ForeignKeyForColumns {

        protected final List<Column> columns;
        protected final ForeignKey key;

        public ForeignKeyForColumns(ForeignKey key, List<Column> columns) {
            this.key = key;
            this.columns = columns;
        }
    }

 }

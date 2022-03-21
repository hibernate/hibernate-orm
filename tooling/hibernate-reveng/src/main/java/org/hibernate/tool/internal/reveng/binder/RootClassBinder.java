package org.hibernate.tool.internal.reveng.binder;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.DuplicateMappingException;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.MetaAttribute;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.Table;
import org.hibernate.tool.api.reveng.TableIdentifier;
import org.hibernate.tool.internal.reveng.RevengMetadataCollector;
import org.hibernate.tool.internal.reveng.util.RevengUtils;

public class RootClassBinder extends AbstractBinder {
	
	private static final Logger LOGGER = Logger.getLogger(RootClassBinder.class.getName());
	
	public static RootClassBinder create(
			BinderContext binderContext) {
		return new RootClassBinder(binderContext);
	}
	
	private final PrimaryKeyBinder primaryKeyBinder;
	private final VersionPropertyBinder versionPropertyBinder;
	private final ForeignKeyBinder foreignKeyBinder;
	private final BasicPropertyBinder basicPropertyBinder;
	
	private RootClassBinder(BinderContext binderContext) {
		super(binderContext);
		this.primaryKeyBinder = PrimaryKeyBinder.create(binderContext);
		this.versionPropertyBinder = VersionPropertyBinder.create(binderContext);
		this.foreignKeyBinder = ForeignKeyBinder.create(binderContext);
		this.basicPropertyBinder = BasicPropertyBinder.create(binderContext);
	}

	public void bind(Table table, RevengMetadataCollector revengMetadataCollector) {
		Set<Column> processed = new HashSet<Column>();
		nullifyDefaultCatalogAndSchema(table);
		RootClass rc = createRootClass(table);
		addToMetadataCollector(rc, table);
		PrimaryKeyInfo pki = bindPrimaryKey(table, rc, processed, revengMetadataCollector);		
		bindVersionProperty(table, rc, processed);
		bindOutgoingForeignKeys(table, rc, processed);
		bindColumnsToProperties(table, rc, processed);
		bindIncomingForeignKeys(rc, processed, revengMetadataCollector);
		updatePrimaryKey(rc, pki);	
	}
	
	private PrimaryKeyInfo bindPrimaryKey(
			Table table, 
			RootClass rc, 
			Set<Column> processed, 
			RevengMetadataCollector revengMetadataCollector) {
		return primaryKeyBinder.bind(table, rc, processed, revengMetadataCollector);	
	}
	
	private void updatePrimaryKey(RootClass rc, PrimaryKeyInfo pki) {
		primaryKeyBinder.updatePrimaryKey(rc, pki);
	}
	
	private void addToMetadataCollector(RootClass rc, Table table) {
		try {
			getMetadataCollector().addEntityBinding(rc);
			getMetadataCollector().addImport( rc.getEntityName(), rc.getEntityName() );
		} catch(DuplicateMappingException dme) {
			// TODO: detect this and generate a "permutation" of it ?
			PersistentClass class1 = getMetadataCollector().getEntityBinding(dme.getName());
			Table table2 = class1.getTable();
			throw new RuntimeException("Duplicate class name '" + rc.getEntityName() + "' generated for '" + table + "'. Same name where generated for '" + table2 + "'");
		}
	}
	
	private RootClass createRootClass(Table table) {
		RootClass rc = new RootClass(getMetadataBuildingContext());
		TableIdentifier tableIdentifier = TableIdentifier.create(table);
		String className = getRevengStrategy().tableToClassName( tableIdentifier );
		LOGGER.log(Level.INFO, "Building entity " + className + " based on " + tableIdentifier);
		rc.setEntityName( className );
		rc.setJpaEntityName( StringHelper.unqualify( className ) );
		rc.setClassName( className );
		rc.setProxyInterfaceName( rc.getEntityName() ); // TODO: configurable ?
		rc.setLazy(true);
		rc.setMetaAttributes(getMetaAttributes(table));
		rc.setDiscriminatorValue( rc.getEntityName() );
		rc.setTable(table);
		rc.setOptimisticLockStyle(OptimisticLockStyle.NONE);
		return rc;
	}
	
	private void nullifyDefaultCatalogAndSchema(Table table) {
		if (table.getCatalog() != null && table.getCatalog().equals(getDefaultCatalog())) {
			table.setCatalog(null);
		}
		if (table.getSchema() != null && table.getSchema().equals(getDefaultSchema())) {
			table.setSchema(null);
		}   		
	}
	
	private void bindVersionProperty(
			Table table, 
			RootClass rc, 
			Set<Column> processed) {
		versionPropertyBinder.bind(table, rc, processed);
	}

	private void bindIncomingForeignKeys(
			PersistentClass rc, 
			Set<Column> processed, 
			RevengMetadataCollector revengMetadataCollector) {
		List<ForeignKey> foreignKeys = revengMetadataCollector.getOneToManyCandidates().get(rc.getEntityName());
		if(foreignKeys!=null) {
			for (Iterator<ForeignKey> iter = foreignKeys.iterator(); iter.hasNext();) {
				foreignKeyBinder.bindIncoming(iter.next(), rc, processed);
			}
		}
	}


	private void bindOutgoingForeignKeys(Table table, RootClass rc, Set<Column> processedColumns) {
		// Iterate the outgoing foreign keys and create many-to-one's
		for (ForeignKey foreignKey : table.getForeignKeys().values()) {
			boolean mutable = true;
            if ( contains( foreignKey.getColumns().iterator(), processedColumns ) ) {
				if ( !preferBasicCompositeIds() ) continue; //it's in the pk, so skip this one
				mutable = false;
            }           
            foreignKeyBinder.bindOutgoing(foreignKey, table, rc, processedColumns, mutable);
		}
	}

	private void bindColumnsToProperties(Table table, RootClass rc, Set<Column> processedColumns) {
		for (Column column : table.getColumns()) {
			if ( !processedColumns.contains(column) ) {
				BinderUtils.checkColumnForMultipleBinding(column);
				String propertyName = getColumnToPropertyNameInRevengStrategy(table, column);				
				Property property = basicPropertyBinder.bind(
						BinderUtils.makeUnique(rc,propertyName), 
						table, 
						column);
				rc.addProperty(property);
			}
		}
	}

    private boolean contains(Iterator<Column> columnIterator, Set<Column> processedColumns) {
        while (columnIterator.hasNext() ) {
            Column element = (Column) columnIterator.next();
            if(processedColumns.contains(element) ) {
                return true;
            }
        }
        return false;
    }

	private Map<String,MetaAttribute> getMetaAttributes(Table table) {
		Map<String,MetaAttribute> result = null;
		TableIdentifier tableIdentifier = TableIdentifier.create(table);
		result = getRevengStrategy().tableToMetaAttributes(tableIdentifier);
		if (result == null) {
			tableIdentifier = RevengUtils.createTableIdentifier(
					table, 
					getDefaultCatalog(), 
					getDefaultSchema());
			result = getRevengStrategy().tableToMetaAttributes(tableIdentifier);
		}
		if (result == null) {
			result = Collections.emptyMap();
		}
		return result;
	}
	
	private String getColumnToPropertyNameInRevengStrategy(
			Table table,
			Column column) {
		String result = null;
		String columnName = column.getName();
		TableIdentifier tableIdentifier = TableIdentifier.create(table);
		result = getRevengStrategy().columnToPropertyName(tableIdentifier, columnName);
		if (result == null) {
			tableIdentifier = RevengUtils.createTableIdentifier(table, getDefaultCatalog(), getDefaultSchema());
			result = getRevengStrategy().columnToPropertyName(tableIdentifier, columnName);
		}
		return result;
	}

}

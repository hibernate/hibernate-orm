package org.hibernate.tool.internal.reveng.binder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.MetaAttribute;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Table;
import org.hibernate.tool.api.reveng.DatabaseCollector;
import org.hibernate.tool.api.reveng.ReverseEngineeringStrategy;
import org.hibernate.tool.api.reveng.TableIdentifier;
import org.hibernate.tool.internal.reveng.PrimaryKeyInfo;
import org.hibernate.tool.internal.reveng.RevEngUtils;

public class PrimaryKeyBinder {
	
	private static final Logger LOGGER = Logger.getLogger(PrimaryKeyBinder.class.getName());
	
	public static PrimaryKeyBinder create(
			MetadataBuildingContext metadataBuildingContext,
			InFlightMetadataCollector metadataCollector,
			ReverseEngineeringStrategy revengStrategy,
			String defaultCatalog,
			String defaultSchema,
			boolean preferBasicCompositeIds) {
		return new PrimaryKeyBinder(
				metadataBuildingContext,
				metadataCollector,
				revengStrategy,
				defaultCatalog,
				defaultSchema,
				preferBasicCompositeIds);
	}
	
	private final MetadataBuildingContext metadataBuildingContext;
	private final InFlightMetadataCollector metadataCollector;
	private final ReverseEngineeringStrategy revengStrategy;
	private final String defaultCatalog;
	private final String defaultSchema;
	private final boolean preferBasicCompositeIds;

	
	private PrimaryKeyBinder(
			MetadataBuildingContext metadataBuildingContext,
			InFlightMetadataCollector metadataCollector,
			ReverseEngineeringStrategy revengStrategy,
			String defaultCatalog,
			String defaultSchema,
			boolean preferBasicCompositeIds) {
		this.metadataBuildingContext = metadataBuildingContext;
		this.metadataCollector = metadataCollector;
		this.revengStrategy = revengStrategy;
		this.defaultCatalog = defaultCatalog;
		this.defaultSchema = defaultSchema;
		this.preferBasicCompositeIds = preferBasicCompositeIds;
	}

	public PrimaryKeyInfo bind(
			Table table, 
			RootClass rc, 
			Set<Column> processed, 
			Mapping mapping, 
			DatabaseCollector collector) {
		
		SimpleValue id = null;
		String idPropertyname = null;

		PrimaryKeyInfo pki = new PrimaryKeyInfo();

		List<Column> keyColumns = null;
		if (table.getPrimaryKey()!=null) {
			keyColumns = table.getPrimaryKey().getColumns();
		}
		else {
			LOGGER.log(Level.INFO, "No primary key found for " + table + ", using all properties as the identifier.");
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
			LOGGER.log(Level.INFO, "id strategy for " + rc.getEntityName() + " since it has a multiple column primary key");
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
			BinderUtils.checkColumnForMultipleBinding(pkc);

			id = SimpleValueBinder.bind(
					metadataBuildingContext, 
					metadataCollector, 
					revengStrategy, 
					table, 
					pkc, 
					mapping, 
					!naturalId);

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

		Property property = PropertyBinder.bind(
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

	private SimpleValue handleCompositeKey(
			RootClass rc, 
			Set<Column> processedColumns, 
			List<Column> keyColumns, 
			Mapping mapping) {
		Component pkc = new Component(metadataBuildingContext, rc);
        pkc.setMetaAttributes(Collections.EMPTY_MAP);
        pkc.setEmbedded(false);

        String compositeIdName = revengStrategy.tableToCompositeIdName(TableIdentifier.create(rc.getTable()));
        if(compositeIdName==null) {
        	compositeIdName = revengStrategy.classNameToCompositeIdName(rc.getClassName());
        }
        pkc.setComponentClassName(compositeIdName);
		Table table = rc.getTable();
        List<?> list = null;
		if (preferBasicCompositeIds ) {
            list = new ArrayList<Object>(keyColumns);
        }
		else {
            list = ForeignKeyUtils.findForeignKeys(table.getForeignKeyIterator(), keyColumns);
        }
        for (Iterator<?> iter = list.iterator(); iter.hasNext();) {
            Object element = iter.next();
			Property property;
            if (element instanceof Column) {
                Column column = (Column) element;
                if ( processedColumns.contains(column) ) {
                    throw new RuntimeException("Binding column twice for primary key should not happen: " + column);
                }
				else {
					BinderUtils.checkColumnForMultipleBinding(column);
                    String propertyName = revengStrategy.columnToPropertyName( TableIdentifier.create(table), column.getName() );
                    property = BasicPropertyBinder
                    		.create(
                    				metadataBuildingContext, 
                    				metadataCollector, 
                    				revengStrategy, 
                    				defaultCatalog, 
                    				defaultSchema)
                    		.bind(
                    				BinderUtils.makeUnique(pkc, propertyName), 
                    				table, 
                    				column, 
                    				mapping);
                    processedColumns.add(column);
                }
            }
			else if (element instanceof ForeignKeyUtils.ForeignKeyForColumns) {
				ForeignKeyUtils.ForeignKeyForColumns fkfc = (ForeignKeyUtils.ForeignKeyForColumns) element;
                ForeignKey foreignKey = fkfc.key;
                String propertyName = revengStrategy.foreignKeyToEntityName(
						foreignKey.getName(),
						TableIdentifier.create(foreignKey.getTable() ),
						foreignKey.getColumns(), TableIdentifier.create(foreignKey.getReferencedTable() ), foreignKey.getReferencedColumns(), true
					);
                property = ManyToOneBinder
                		.create(
                				metadataBuildingContext, 
                				revengStrategy,
                				defaultCatalog, 
                				defaultSchema)
                		.bind(
                				BinderUtils.makeUnique(pkc, propertyName), 
                				true, 
                				table, 
                				foreignKey, 
                				processedColumns);
                processedColumns.addAll(fkfc.columns);
            }
			else {
				throw new RuntimeException("unknown thing");
			}

            markAsUseInEquals(property);
            pkc.addProperty(property);

		}

		return pkc;
	}

    private static void markAsUseInEquals(Property property) {
        Map<String, MetaAttribute> m = new HashMap<String, MetaAttribute>();
        MetaAttribute ma = new MetaAttribute("use-in-equals");
        ma.addValue("true");
        m.put(ma.getName(),ma);
        property.setMetaAttributes(m);
    }

}

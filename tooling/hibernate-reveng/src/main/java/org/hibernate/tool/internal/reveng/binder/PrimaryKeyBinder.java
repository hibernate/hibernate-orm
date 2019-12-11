package org.hibernate.tool.internal.reveng.binder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.MetaAttribute;
import org.hibernate.mapping.OneToOne;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Table;
import org.hibernate.tool.api.reveng.DatabaseCollector;
import org.hibernate.tool.api.reveng.TableIdentifier;
import org.hibernate.tool.internal.reveng.DefaultAssociationInfo;
import org.hibernate.tool.internal.reveng.PrimaryKeyInfo;
import org.hibernate.tool.internal.reveng.RevEngUtils;

class PrimaryKeyBinder extends AbstractBinder {
	
	private static final Logger LOGGER = Logger.getLogger(PrimaryKeyBinder.class.getName());
	
	static PrimaryKeyBinder create(BinderContext binderContext) {
		return new PrimaryKeyBinder(binderContext);
	}
	
	private final BasicPropertyBinder basicPropertyBinder;
	private final SimpleValueBinder simpleValueBinder;
	private final ManyToOneBinder manyToOneBinder;
	private final PropertyBinder propertyBinder;

	
	private PrimaryKeyBinder(BinderContext binderContext) {
		super(binderContext);
		this.basicPropertyBinder = BasicPropertyBinder.create(binderContext);
		this.simpleValueBinder = SimpleValueBinder.create(binderContext);
		this.manyToOneBinder = ManyToOneBinder.create(binderContext);
		this.propertyBinder = PropertyBinder.create(binderContext);
	}

	PrimaryKeyInfo bind(
			Table table, 
			RootClass rc, 
			Set<Column> processed, 
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

			id = handleCompositeKey(rc, processed, keyColumns);
			idPropertyname = getRevengStrategy().tableToIdentifierPropertyName(tableIdentifier);
			if(idPropertyname==null) {
				idPropertyname = "id";
			}
		}
		else {
			pki.suggestedStrategy = RevEngUtils.getTableIdentifierStrategyNameInRevengStrategy(getRevengStrategy(), table, getDefaultCatalog(), getDefaultSchema());
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

			id = simpleValueBinder.bind(
					table, 
					pkc, 
					!naturalId);

			idPropertyname = getRevengStrategy().tableToIdentifierPropertyName(tableIdentifier);
			if(idPropertyname==null) {
				idPropertyname = getRevengStrategy().columnToPropertyName(tableIdentifier, pkc.getName() );
			}

			processed.add(pkc);
		}
		id.setIdentifierGeneratorStrategy(tableIdentifierStrategyName);
		pki.suggestedProperties = getRevengStrategy().getTableIdentifierProperties(tableIdentifier);
		id.setIdentifierGeneratorProperties(pki.suggestedProperties);
		if(naturalId) {
			id.setNullValue("undefined");
		}

		Property property = propertyBinder.bind(
				table, 
				BinderUtils.makeUnique(rc,idPropertyname), 
				id, 
				DefaultAssociationInfo.create(null, null, true, true));
		rc.setIdentifierProperty(property);
		rc.setIdentifier(id);

		return pki;
	}

	void updatePrimaryKey(RootClass rc, PrimaryKeyInfo pki) {
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

	private SimpleValue handleCompositeKey(
			RootClass rc, 
			Set<Column> processedColumns, 
			List<Column> keyColumns) {
		Component pkc = new Component(getMetadataBuildingContext(), rc);
        pkc.setMetaAttributes(Collections.EMPTY_MAP);
        pkc.setEmbedded(false);

        String compositeIdName = getRevengStrategy().tableToCompositeIdName(TableIdentifier.create(rc.getTable()));
        if(compositeIdName==null) {
        	compositeIdName = getRevengStrategy().classNameToCompositeIdName(rc.getClassName());
        }
        pkc.setComponentClassName(compositeIdName);
		Table table = rc.getTable();
        List<?> list = null;
		if (preferBasicCompositeIds() ) {
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
                    String propertyName = getRevengStrategy().columnToPropertyName( TableIdentifier.create(table), column.getName() );
                    property = basicPropertyBinder.bind(
            				BinderUtils.makeUnique(pkc, propertyName), 
            				table, 
            				column);
                    processedColumns.add(column);
                }
            }
			else if (element instanceof ForeignKeyUtils.ForeignKeyForColumns) {
				ForeignKeyUtils.ForeignKeyForColumns fkfc = (ForeignKeyUtils.ForeignKeyForColumns) element;
                ForeignKey foreignKey = fkfc.key;
                String propertyName = getRevengStrategy().foreignKeyToEntityName(
						foreignKey.getName(),
						TableIdentifier.create(foreignKey.getTable() ),
						foreignKey.getColumns(), TableIdentifier.create(foreignKey.getReferencedTable() ), foreignKey.getReferencedColumns(), true
					);
                property = manyToOneBinder
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

}

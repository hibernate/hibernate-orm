/*
 * Created on 2004-12-03
 */
package org.hibernate.tool.internal.export.hbm;

import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.hibernate.FetchMode;
import org.hibernate.boot.Metadata;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryCollectionReturn;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryJoinReturn;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryReturn;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryRootReturn;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.engine.spi.NamedQueryDefinition;
import org.hibernate.engine.spi.NamedSQLQueryDefinition;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.mapping.Any;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Formula;
import org.hibernate.mapping.JoinedSubclass;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.OneToMany;
import org.hibernate.mapping.OneToOne;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.PersistentClassVisitor;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.SingleTableSubclass;
import org.hibernate.mapping.Subclass;
import org.hibernate.mapping.UnionSubclass;
import org.hibernate.mapping.Value;
import org.hibernate.persister.entity.JoinedSubclassEntityPersister;
import org.hibernate.persister.entity.SingleTableEntityPersister;
import org.hibernate.persister.entity.UnionSubclassEntityPersister;
import org.hibernate.tool.internal.export.common.EntityNameFromValueVisitor;
import org.hibernate.tool.internal.util.SkipBackRefPropertyIterator;

/**
 * @author David Channon and Max
 */
public class Cfg2HbmTool {

	private final class HasEntityPersisterVisitor implements PersistentClassVisitor {
		private final String name;

		private HasEntityPersisterVisitor(String name) {
			this.name = name;
		}

		public Object accept(Subclass subclass) {
			return bool(!SingleTableEntityPersister.class.getName().equals(name));
		}

		private Object bool(boolean b) {
			return Boolean.valueOf( b );
		}

		public Object accept(JoinedSubclass subclass) {
			return bool(!JoinedSubclassEntityPersister.class.getName().equals(name));
		}

		public Object accept(SingleTableSubclass subclass) {
			return bool(!SingleTableEntityPersister.class.getName().equals(name));
		}

		public Object accept(UnionSubclass subclass) {
			return bool(!UnionSubclassEntityPersister.class.getName().equals(name));
		}

		public Object accept(RootClass class1) {
			return bool(!SingleTableEntityPersister.class.getName().equals(name));
		}
	}

	/**
	 * Remove any internal keys from the set, eg, any Keys that are prefixed by
	 * 'target_', {@link PersistentIdentifierGenerator.IDENTIFIER_NORMALIZER} and return the filtered collection.
	 *
	 * @param properties
	 * @return
	 */
	public static Properties getFilteredIdentifierGeneratorProperties(Properties properties, Properties environmentProperties) {
		if (properties != null){
			Properties fProp = new Properties();
			Iterator<?> itr = properties.keySet().iterator();
			while (itr.hasNext() ) {
				String key = (String) itr.next();
				if ("schema".equals(key)) {
					String schema = properties.getProperty(key);
					if (!isDefaultSchema(schema, environmentProperties)) {
						fProp.put(key, schema);
					} 
				} else if ("catalog".equals(key)) {
					String catalog = properties.getProperty(key);
					if (!isDefaultCatalog(catalog, environmentProperties)) {
						fProp.put(key, catalog);
					}
				} else if (! (key.startsWith("target_") 
						|| key.equals(PersistentIdentifierGenerator.IDENTIFIER_NORMALIZER))) {
					fProp.put(key, properties.get(key));
				}
			}
			return fProp;
		}
		return null;
	}
	
	static private boolean isDefaultSchema(String schema, Properties properties) {
		String defaultSchema = properties.getProperty(Environment.DEFAULT_SCHEMA);
		return defaultSchema == null ? schema == null : defaultSchema.equals(schema);
	}

	static private boolean isDefaultCatalog(String catalog, Properties properties) {
		String defaultCatalog = properties.getProperty(Environment.DEFAULT_CATALOG);
		return defaultCatalog == null ? catalog == null : defaultCatalog.equals(catalog);
	}

	public String getTag(PersistentClass pc) {
		return (String) pc.accept(HBMTagForPersistentClassVisitor.INSTANCE);
	}

	public String getTag(Property property) {
		PersistentClass persistentClass = property.getPersistentClass();
		if(persistentClass!=null) {
			if(persistentClass.getVersion()==property) {
				String typeName = ((SimpleValue)property.getValue()).getTypeName();
				if("timestamp".equals(typeName) || "dbtimestamp".equals(typeName)) {
					return "timestamp";
				} else {
					return "version";
				}
			}
		}
		String toolTag = (String) property.getValue().accept(HBMTagForValueVisitor.INSTANCE);
		if ("component".equals(toolTag) && "embedded".equals(property.getPropertyAccessorName())){
			toolTag = "properties";
		}
		return toolTag;
	}

	public String getCollectionElementTag(Property property){
		Value value = property.getValue();
		if (isOneToMany(value)) return "one-to-many";
		if (isManyToMany(value)) return "many-to-many";
		if (isManyToAny(value)) return "many-to-any";
		if (((Collection)value).getElement() instanceof Component){
			return "composite";
		}
		return "element";
	}


	public boolean isUnsavedValue(Property property) {
		SimpleValue sv = (SimpleValue) property.getValue();
		return ((sv.getNullValue()==null) || "undefined".equals(sv.getNullValue())) ? false : true;
	}

	public String getUnsavedValue(Property property) {
		return ( (SimpleValue) property.getValue() ).getNullValue();
	}

	/**
	 *
	 * @param property
	 * @return
	 */
	public boolean isIdentifierGeneratorProperties(Property property) {
		Properties val = this.getIdentifierGeneratorProperties(property);
		return (val==null) ? false : true;
	}
	
	public Properties getIdentifierGeneratorProperties(Property property) {
		return ( (SimpleValue) property.getValue() ).getIdentifierGeneratorProperties();
	}
	
	/**
	 * @param property
	 * @return
	 */
	public Set<?> getFilteredIdentifierGeneratorKeySet(Property property, Properties props) {
		return getFilteredIdentifierGeneratorProperties(this.getIdentifierGeneratorProperties(property), props).keySet();
	}

    public boolean isOneToMany(Property property) {
        return isOneToMany(property.getValue());
    }

    public boolean isOneToMany(Value value) {
        if(value instanceof Collection) {
            return ( (Collection)value ).isOneToMany();
        }else if(value instanceof OneToMany){
        	return true;
        }
        return false;
    }

        public boolean isManyToMany(Property property) {
   		return isManyToMany(property.getValue());
    }

    public boolean isManyToMany(Value value) {
		return	(value instanceof Collection &&
    			((Collection)value).getElement() instanceof ManyToOne);
    }


	public boolean isCollection(Property property) {
        return property.getValue() instanceof Collection;
    }

	public boolean isOneToManyCollection(Property property) {
		return isCollection(property) && ((Collection)property.getValue()).isOneToMany();
	}

	public boolean isSimpleValue(Property property) {
        return (property.getValue() instanceof SimpleValue);
	}

	public boolean isManyToOne(Property property) {
        return isManyToOne(property.getValue());
    }

	public boolean isManyToAny(Property property) {
        return isManyToAny(property.getValue());
    }

	public boolean isManyToAny(Value value) {
        return (value instanceof Collection &&
    			((Collection)value).getElement() instanceof Any);
    }

	public boolean isManyToOne(Value value) {
        return (value instanceof ManyToOne);
    }

	public boolean isOneToOne(Property property) {
        return (property.getValue() instanceof OneToOne);
    }

	public boolean isTemporalValue(Property property) {
		if(property.getValue() instanceof SimpleValue) {
			String typeName = ((SimpleValue)property.getValue()).getTypeName();
			if("date".equals(typeName) || "java.sql.Date".equals(typeName)) {
				return true;
			} else if ("timestamp".equals(typeName) || "java.sql.Timestamp".equals(typeName)) {
				return true;
			} else if ("time".equals(typeName) || "java.sql.Time".equals(typeName)) {
				return true;
			}
		}
		return false;
	}

    public boolean isNamedQueries(Metadata md) {
		java.util.Collection<NamedQueryDefinition> nqry = md.getNamedQueryDefinitions();
		return nqry == null || nqry.isEmpty() ? false : true;
	}

	public boolean isNamedSQLQueries(Metadata md) {
		java.util.Collection<NamedSQLQueryDefinition> nsqlqry = md.getNamedNativeQueryDefinitions();
		return nsqlqry == null || nsqlqry.isEmpty() ? false : true;
	}


	public String getCollectionLazy(Collection value){
		return value.isExtraLazy() ? "extra" : Boolean.toString(value.isLazy());
	}

	public String getNamedSQLReturnTag(NativeSQLQueryReturn sqlret) {
		String retVal = "return";
		if (isNamedSQLReturnRole(sqlret) ) {
			retVal = "return-join";
		}
		else if (isNamedSQLReturnCollection(sqlret) ) {
			retVal = "load-collection";
		}
		return retVal;
	}

	public String getNamedSQLReturnProperty(NativeSQLQueryJoinReturn o) {
		/*if(o instanceof NativeSQLQueryCollectionReturn) {
			return ((NativeSQLQueryCollectionReturn)o).getOwnerEntityName() + "." + ((NativeSQLQueryCollectionReturn)o).getOwnerProperty();
		}*/
		return o.getOwnerAlias() + "." + o.getOwnerProperty();
	}

	public String getNamedSQLReturnRole(NativeSQLQueryCollectionReturn o) {
		return o.getOwnerEntityName() + "." + o.getOwnerProperty();
	}

	public boolean isNamedSQLReturnRoot(NativeSQLQueryReturn sqlret) {
		return sqlret instanceof NativeSQLQueryRootReturn;
	}

	public boolean isNamedSQLReturnCollection(NativeSQLQueryReturn sqlret) {
		return sqlret instanceof NativeSQLQueryCollectionReturn;
	}

	public boolean isNamedSQLReturnRole(NativeSQLQueryReturn sqlret) {
		return sqlret instanceof NativeSQLQueryJoinReturn;
	}

	public boolean isFilterDefinitions(Metadata md) {
		Map<String, FilterDefinition> filterdefs = md.getFilterDefinitions();
		return filterdefs == null || filterdefs.isEmpty() ? false : true;
	}

	public boolean isClassLevelOptimisticLockMode(PersistentClass pc) {
		return pc.getOptimisticLockStyle() != OptimisticLockStyle.VERSION;
	}

	public String getClassLevelOptimisticLockMode(PersistentClass pc) {
		OptimisticLockStyle oMode = pc.getOptimisticLockStyle();
		if ( oMode == OptimisticLockStyle.DIRTY ) {
			return "dirty";
		}
		else if ( oMode == OptimisticLockStyle.ALL ) {
			return "all";
		}
		else if ( oMode == OptimisticLockStyle.NONE ) {
			return "none";
		}
		else {
			return "version";
		}
	}

	public boolean hasFetchMode(Property property) {
		String fetch = getFetchMode(property);
		if(fetch==null || "default".equals(fetch)) {
			return false;
		} else {
			return true;
		}
	}
	public String getFetchMode(Property property) {
		FetchMode fetchMode = property.getValue().getFetchMode();
		return (fetchMode== null) ? null : fetchMode.toString().toLowerCase();
	}


	public Formula getFormulaForProperty(Property prop) {
		Iterator<?> iter = prop.getValue().getColumnIterator();
		while ( iter.hasNext() ) {
			Object o = iter.next();
			if (o instanceof Formula)
				return (Formula) o;
		}
		return null;
	}

	public String columnAttributes(Column col) {
		return columnAttributes(col, false);
	}

	public String columnAttributes(Column column, boolean isPrimaryKeyColumn) {
		StringBuffer sb = new StringBuffer();
		if (column.getPrecision() != Column.DEFAULT_PRECISION) {
			sb.append("precision=\"").append(column.getPrecision() ).append("\" ");
		}
		if (column.getScale() != Column.DEFAULT_SCALE) {
			sb.append("scale=\"").append(column.getScale() ).append("\" ");
		}
		else if (column.getLength() != Column.DEFAULT_LENGTH){
			sb.append("length=\"").append(column.getLength() ).append("\" ");
		}
		if (!isPrimaryKeyColumn) {
			if (!column.isNullable() ) {
				sb.append("not-null=\"true\" ");
			}
			if (column.isUnique() ) {
				sb.append("unique=\"true\" ");
			}
		}
		if (column.getSqlType() != null) {
			sb.append("sql-type=\""); sb.append(column.getSqlType() ); sb.append("\" ");
		}
		return sb.toString();
	}

	public String getClassName(PersistentClass pc) {
		if (pc.hasPojoRepresentation() ) {
			return pc.getClassName();
		}
		else {
			// todo: return null?
			throw new RuntimeException(pc + " does not have a pojo rep.");
		}
	}

	public String getClassName(OneToMany om) {
		return om.getAssociatedClass().getClassName();
	}

	public String getProxyInterfaceName(PersistentClass pc) {
		if (pc.hasPojoRepresentation() ) {
			return pc.getProxyInterfaceName();
		}
		else {
			throw new RuntimeException(pc + " does not have a pojo rep.");
		}
	}

	public boolean isImportData(Metadata md) {
		return !(md.getImports().isEmpty());
	}

 	public boolean needsDiscriminatorElement(PersistentClass clazz) {
 			return clazz instanceof RootClass
 			  && (clazz.getDiscriminator() != null);
 		}
 		  		
	public boolean needsDiscriminator(PersistentClass clazz) {

		return clazz instanceof Subclass
		  && !(clazz instanceof UnionSubclass) && !(clazz instanceof JoinedSubclass);
	}


	public boolean needsTable(PersistentClass clazz) {
		Boolean accept = (Boolean) clazz.accept(new PersistentClassVisitor(){
		
			public Object accept(Subclass subclass) {
				return Boolean.FALSE;
			}
		
			public Object accept(JoinedSubclass subclass) {
				return Boolean.TRUE;
			}
		
			public Object accept(SingleTableSubclass subclass) {
				return Boolean.FALSE;
			}
		
			public Object accept(UnionSubclass subclass) {
				return Boolean.TRUE;
			}
		
			public Object accept(RootClass class1) {
				return Boolean.TRUE;
			}
		});						
		
		return accept.booleanValue();
	}

	public boolean isSubclass(PersistentClass clazz) {
		return clazz instanceof org.hibernate.mapping.Subclass;
	}

	public boolean isJoinedSubclass(PersistentClass clazz) {
		return clazz instanceof JoinedSubclass;
	}

	public boolean hasCustomEntityPersister(PersistentClass clazz) {
		Class<?> entityPersisterClass = clazz.getEntityPersisterClass();
		if(entityPersisterClass==null) return false;
		final String name = entityPersisterClass.getName();

		Boolean object = (Boolean) clazz.accept( new HasEntityPersisterVisitor( name ) );
		return object.booleanValue();
	}

	public String getHibernateTypeName(Property p) {
		return (String) p.getValue().accept(new EntityNameFromValueVisitor());
	}


	public String getSafeHibernateTypeName(Property p) {
		return (String) p.getValue().accept(new EntityNameFromValueVisitor(false));
	}

	public Iterator<?> getProperties(Component v) {
		return new SkipBackRefPropertyIterator(v.getPropertyIterator());
	}

	public Iterator<?> getProperties(PersistentClass pc) {
		return new SkipBackRefPropertyIterator(pc.getUnjoinedPropertyIterator());
	}
}

package org.hibernate.tool.internal.dialect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.tool.api.dialect.MetaDataDialect;
import org.hibernate.tool.api.reveng.ReverseEngineeringRuntimeInfo;

public class CachedMetaDataDialect implements MetaDataDialect {
	
	MetaDataDialect delegate;
	private Map<StringKey, List<Map<String, Object>>> cachedTables = new HashMap<StringKey, List<Map<String, Object>>>();
	private Map<StringKey, List<Map<String, Object>>> cachedColumns = new HashMap<StringKey, List<Map<String, Object>>>();
	private Map<StringKey, List<Map<String, Object>>> cachedExportedKeys = new HashMap<StringKey, List<Map<String, Object>>>();
	private Map<StringKey, List<Map<String, Object>>> cachedPrimaryKeys = new HashMap<StringKey, List<Map<String, Object>>>();
	private Map<StringKey, List<Map<String, Object>>> cachedIndexInfo = new HashMap<StringKey, List<Map<String, Object>>>();
	private Map<StringKey, List<Map<String, Object>>> cachedPrimaryKeyStrategyName = new HashMap<StringKey, List<Map<String, Object>>>();

	public CachedMetaDataDialect(MetaDataDialect realMetaData) {
		this.delegate = realMetaData;
	}
	
	public void close() {
		delegate.close();
	}

	public void configure(ReverseEngineeringRuntimeInfo info) {
        delegate.configure(info);       
    }
	
	public void close(Iterator<?> iterator) {
		if(iterator instanceof CachedIterator) {
			CachedIterator ci = (CachedIterator) iterator;
			if(ci.getOwner()==this) {
				ci.store();
				return;
			} 
		}
		delegate.close( iterator );
	}

	

	public Iterator<Map<String, Object>> getColumns(String catalog, String schema, String table, String column) {
		StringKey sk = new StringKey(new String[] { catalog, schema, table, column });
		List<Map<String, Object>> cached = cachedColumns.get( sk );
		if(cached==null) {
			cached = new ArrayList<Map<String, Object>>();
			return new CachedIterator(this, cachedColumns, sk, cached, delegate.getColumns( catalog, schema, table, column ));
		} else {
			return cached.iterator();
		}		
	}

	public Iterator<Map<String, Object>> getExportedKeys(String catalog, String schema, String table) {
		StringKey sk = new StringKey(new String[] { catalog, schema, table });
		List<Map<String, Object>> cached = cachedExportedKeys.get( sk );
		if(cached==null) {
			cached = new ArrayList<Map<String, Object>>();
			return new CachedIterator(this, cachedExportedKeys, sk, cached, delegate.getExportedKeys( catalog, schema, table ));
		} else {
			return cached.iterator();
		}		
	}

	public Iterator<Map<String, Object>> getIndexInfo(String catalog, String schema, String table) {
		StringKey sk = new StringKey(new String[] { catalog, schema, table });
		List<Map<String, Object>> cached = cachedIndexInfo.get( sk );
		if(cached==null) {
			cached = new ArrayList<Map<String, Object>>();
			return new CachedIterator(this, cachedIndexInfo, sk, cached, delegate.getIndexInfo( catalog, schema, table ));
		} else {
			return cached.iterator();
		}
	}

	public Iterator<Map<String, Object>> getPrimaryKeys(String catalog, String schema, String name) {
		StringKey sk = new StringKey(new String[] { catalog, schema, name });
		List<Map<String, Object>> cached = cachedPrimaryKeys .get( sk );
		if(cached==null) {
			cached = new ArrayList<Map<String, Object>>();
			return new CachedIterator(this, cachedPrimaryKeys, sk, cached, delegate.getPrimaryKeys( catalog, schema, name ));
		} else {
			return cached.iterator();
		}
	}

	public Iterator<Map<String, Object>> getTables(String catalog, String schema, String table) {
		StringKey sk = new StringKey(new String[] { catalog, schema, table });
		List<Map<String, Object>> cached = cachedTables.get( sk );
		if(cached==null) {
			cached = new ArrayList<Map<String, Object>>();
			return new CachedIterator(this, cachedTables, sk, cached, delegate.getTables( catalog, schema, table ));
		} else {
			return cached.iterator();
		}
	}

	public Iterator<Map<String, Object>> getSuggestedPrimaryKeyStrategyName(String catalog, String schema, String table) {
		StringKey sk = new StringKey(new String[] { catalog, schema, table });
		List<Map<String, Object>> cached = cachedPrimaryKeyStrategyName.get( sk );
		if(cached==null) {
			cached = new ArrayList<Map<String, Object>>();
			return new CachedIterator(this, cachedPrimaryKeyStrategyName, sk, cached, delegate.getSuggestedPrimaryKeyStrategyName( catalog, schema, table ));
		} else {
			return cached.iterator();
		}
	}
	
	public boolean needQuote(String name) {
		return delegate.needQuote( name );
	}
	
	private static class StringKey {
		String[] keys;
		
		StringKey(String[] key) {
			this.keys=key;
		}
		
		public int hashCode() {
			if (keys == null)
	            return 0;
	 
	        int result = 1;
	 
	        for (int i = 0; i < keys.length; i++) {
				Object element = keys[i];
			    result = 31 * result + (element == null ? 0 : element.hashCode());
	        }
	        
	        return result;	 
		}
		
		public boolean equals(Object obj) {
			StringKey other = (StringKey) obj;
			String[] otherKeys = other.keys;
			
			if(otherKeys.length!=keys.length) {
				return false;
			}
			
			for (int i = otherKeys.length-1; i >= 0; i--) {
				if(!safeEquals(otherKeys[i],(keys[i]))) {
					return false;
				}
			}
			
			return true;
		}
		
		private boolean safeEquals(Object obj1, Object obj2) {
			if ( obj1 == null ) {
				return obj2 == null;
			}
	        return obj1.equals( obj2 );
		}
	}
	
	private static class CachedIterator implements Iterator<Map<String, Object>> {

		private List<Map<String, Object>> cache; 
		private StringKey target;
		private Map<StringKey, List<Map<String, Object>>> destination;
		private Iterator<Map<String, Object>> realIterator;
		final CachedMetaDataDialect owner;
		public CachedIterator(CachedMetaDataDialect owner, Map<StringKey, List<Map<String, Object>>> destination, StringKey sk, List<Map<String, Object>> cache, Iterator<Map<String, Object>> realIterator) {
			this.owner = owner;
			this.destination = destination;
			this.target = sk;
			this.realIterator = realIterator;
			this.cache = cache;
		}
		
		public CachedMetaDataDialect getOwner() {
			return owner;
		}

		public boolean hasNext() {			
			return realIterator.hasNext();
		}

		public Map<String, Object> next() {
			Map<String, Object> map = realIterator.next();
			cache.add(new HashMap<String, Object>(map)); // need to copy since MetaDataDialect might reuse it.
			return map;
		}

		public void remove() {
			realIterator.remove();
		}

		public void store() {
			destination.put( target, cache );
			if(realIterator.hasNext()) throw new IllegalStateException("CachedMetaDataDialect have not been fully initialized!");
			cache = null;
			target = null;
			destination = null;
			realIterator = null;			
		}
	}

	

		
}

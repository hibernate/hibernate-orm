/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2010-2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.internal.reveng.dialect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.tool.api.reveng.RevengDialect;

public class CachedMetaDataDialect implements RevengDialect {

    RevengDialect delegate;
    private final Map<StringKey, List<Map<String, Object>>> cachedTables = new HashMap<>();
    private final Map<StringKey, List<Map<String, Object>>> cachedColumns = new HashMap<>();
    private final Map<StringKey, List<Map<String, Object>>> cachedExportedKeys = new HashMap<>();
    private final Map<StringKey, List<Map<String, Object>>> cachedPrimaryKeys = new HashMap<>();
    private final Map<StringKey, List<Map<String, Object>>> cachedIndexInfo = new HashMap<>();
    private final Map<StringKey, List<Map<String, Object>>> cachedPrimaryKeyStrategyName = new HashMap<>();

    public CachedMetaDataDialect(RevengDialect realMetaData) {
        this.delegate = realMetaData;
    }

    public void close() {
        delegate.close();
    }

    public void configure(
            ConnectionProvider connectionProvider) {
        delegate.configure(connectionProvider);
    }

    public void close(Iterator<?> iterator) {
        if( iterator instanceof CachedIterator ci ) {
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
            cached = new ArrayList<>();
            return new CachedIterator(this, cachedColumns, sk, cached, delegate.getColumns( catalog, schema, table, column ));
        }
        else {
            return cached.iterator();
        }
    }

    public Iterator<Map<String, Object>> getExportedKeys(String catalog, String schema, String table) {
        StringKey sk = new StringKey(new String[] { catalog, schema, table });
        List<Map<String, Object>> cached = cachedExportedKeys.get( sk );
        if(cached==null) {
            cached = new ArrayList<>();
            return new CachedIterator(this, cachedExportedKeys, sk, cached, delegate.getExportedKeys( catalog, schema, table ));
        }
        else {
            return cached.iterator();
        }
    }

    public Iterator<Map<String, Object>> getIndexInfo(String catalog, String schema, String table) {
        StringKey sk = new StringKey(new String[] { catalog, schema, table });
        List<Map<String, Object>> cached = cachedIndexInfo.get( sk );
        if(cached==null) {
            cached = new ArrayList<>();
            return new CachedIterator(this, cachedIndexInfo, sk, cached, delegate.getIndexInfo( catalog, schema, table ));
        }
        else {
            return cached.iterator();
        }
    }

    public Iterator<Map<String, Object>> getPrimaryKeys(String catalog, String schema, String name) {
        StringKey sk = new StringKey(new String[] { catalog, schema, name });
        List<Map<String, Object>> cached = cachedPrimaryKeys .get( sk );
        if(cached==null) {
            cached = new ArrayList<>();
            return new CachedIterator(this, cachedPrimaryKeys, sk, cached, delegate.getPrimaryKeys( catalog, schema, name ));
        }
        else {
            return cached.iterator();
        }
    }

    public Iterator<Map<String, Object>> getTables(String catalog, String schema, String table) {
        StringKey sk = new StringKey(new String[] { catalog, schema, table });
        List<Map<String, Object>> cached = cachedTables.get( sk );
        if(cached==null) {
            cached = new ArrayList<>();
            return new CachedIterator(this, cachedTables, sk, cached, delegate.getTables( catalog, schema, table ));
        }
        else {
            return cached.iterator();
        }
    }

    public Iterator<Map<String, Object>> getSuggestedPrimaryKeyStrategyName(String catalog, String schema, String table) {
        StringKey sk = new StringKey(new String[] { catalog, schema, table });
        List<Map<String, Object>> cached = cachedPrimaryKeyStrategyName.get( sk );
        if(cached==null) {
            cached = new ArrayList<>();
            return new CachedIterator(this, cachedPrimaryKeyStrategyName, sk, cached, delegate.getSuggestedPrimaryKeyStrategyName( catalog, schema, table ));
        }
        else {
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

            for ( Object element : keys ) {
                result = 31 * result + (element == null ? 0 : element.hashCode());
            }

            return result;
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof StringKey other)) return false;
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
            cache.add( new HashMap<>( map )); // need to copy since MetaDataDialect might reuse it.
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

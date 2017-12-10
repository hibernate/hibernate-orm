/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.springcache;

import java.util.HashMap; import java.util.Iterator; import java.util.Map; import java.util.Objects; import java.util.Set; import java.util.concurrent.atomic.AtomicBoolean;

import javax.cache.Cache; import javax.cache.CacheManager; import javax.cache.configuration.CacheEntryListenerConfiguration; import javax.cache.configuration.Configuration; import 
javax.cache.integration.CompletionListener; import javax.cache.processor.EntryProcessor; import javax.cache.processor.EntryProcessorException; import javax.cache.processor.EntryProcessorResult; import 
javax.cache.processor.MutableEntry;

import org.springframework.cache.Cache.ValueWrapper; import org.springframework.core.GenericTypeResolver;

class SpringCacheToJCache<K, V> implements Cache<K, V> {
	private final org.springframework.cache.Cache springCache;
	private final CacheManager cacheManager;
	private final Class<V> valueClass;

	private final AtomicBoolean closed = new AtomicBoolean(false);

	private class CacheEntry implements MutableEntry<K, V> {
		private K key;
		private V value;

		public CacheEntry(final K key, final V value) {
			super();
			this.key = key;
			this.value = value;
		}

		@Override
		public K getKey() {
			return key;
		}

		@Override
		public V getValue() {
			// The javadoc for javax.cache.Cache.Entry.getValue() says:
			// Returns the value stored in the cache when this entry was created.
			// so don't get the live value
			return value;
		}

		@Override
		public <T> T unwrap(final Class<T> clazz) {
			throw new IllegalArgumentException();
		}

		@Override
		public boolean exists() {
			return springCache.get(key)!=null;
		}

		@Override
		public void remove() {
			springCache.evict(key);
		}

		@Override
		public void setValue(final V value) {
			springCache.put(key, value);
		}

	}

	private void checkClosed(){
		if(closed.get()){
			throw new IllegalStateException("Cache is closed");
		}
	}

	@SuppressWarnings("unchecked")
	public SpringCacheToJCache(final CacheManager cacheManager, final org.springframework.cache.Cache springCache) {
		super();
	final Class<?>[] genericArguments = GenericTypeResolver.resolveTypeArguments(
		getClass(), SpringCacheToJCache.class);
	this.cacheManager = cacheManager;
	this.valueClass = (Class<V>) (genericArguments == null ? Object.class : genericArguments[1]);
		this.springCache = springCache;
	}

	@Override
	public V get(final K key) {
		checkClosed();
		return springCache.get(key, valueClass);
	}

	@Override
	public Map<K, V> getAll(final Set<? extends K> keys) {
		checkClosed();
		final Map<K,V> ret = new HashMap<>();
		for(final K key: keys){
			final ValueWrapper valueWrapper = springCache.get(key);
			if(valueWrapper != null){
				// keys that don't exist in the cache should not be present in the returned map
				@SuppressWarnings("unchecked")
				final V value = (V) valueWrapper.get();
				ret.put(key, value);
			}
		}
		return ret;
	}

	@Override
	public boolean containsKey(final K key) {
		checkClosed();
		return springCache.get(key)!=null;
	}

	@Override
	public void loadAll(final Set<? extends K> keys, boolean replaceExistingValues, final CompletionListener completionListener) {
		checkClosed();
		throw new UnsupportedOperationException();
	}

	@Override
	public void put(final K key, final V value) {
		checkClosed();
		springCache.put(key, value);
	}

	@Override
	public V getAndPut(final K key, final V value) {
		checkClosed();
		final V oldValue = springCache.get(key, valueClass);
		springCache.put(key, value);
		return oldValue;
	}

	@Override
	public void putAll(final Map<? extends K, ? extends V> map) {
		checkClosed();
		for(final Map.Entry<? extends K,? extends V> entry : map.entrySet()){
			put(entry.getKey(), entry.getValue());
		}
	}

	@Override
	public boolean putIfAbsent(final K key, final V value) {
		checkClosed();
		final ValueWrapper valueWrapper = springCache.get(key);
		if(valueWrapper == null){
			put(key, value);
			return true;
		}
		else{
			return false;
		}
	}

	@Override
	public boolean remove(final K key) {
		checkClosed();
		final boolean exists = springCache.get(key)!=null;
		springCache.evict(key);
		return exists;
	}

	@Override
	public boolean remove(final K key, final V oldValue) {
		checkClosed();
		if(Objects.equals(get(key), oldValue)){
			springCache.evict(key);
			return true;
		}
		else{
			return false;
		}
	}

	@Override
	public V getAndRemove(final K key) {
		checkClosed();
		if (containsKey(key)) {
			final V oldValue = get(key);
			remove(key);
			return oldValue;
		}
		else {
			return null;
		}
	}

	@Override
	public boolean replace(final K key, final V oldValue, final V newValue) {
		checkClosed();
		final ValueWrapper valueWrapper = springCache.get(key);
		if(valueWrapper == null){
			return false;
		}
		else{
			if(Objects.equals(oldValue, valueWrapper.get())){
				put(key, newValue);
				return true;
			}
			else{
				return false;
			}
		}
	}

	@Override
	public boolean replace(final K key, final V value) {
		checkClosed();
		if (containsKey(key)) {
			put(key, value);
			return true;
		}
		else {
			return false;
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public V getAndReplace(final K key, final V value) {
		checkClosed();
		final ValueWrapper valueWrapper = springCache.get(key);
		if(valueWrapper == null){
			return null;
		}
		else {
			put(key, value);
			return (V) valueWrapper.get();
		}
	}

	@Override
	public void removeAll(final Set<? extends K> keys) {
		checkClosed();
		for(final K key : keys){
			springCache.evict(key);
		}
	}

	@Override
	public void removeAll() {
		checkClosed();
		springCache.clear();
	}

	@Override
	public void clear() {
		checkClosed();
		springCache.clear();
	}

	@Override
	public <C extends Configuration<K, V>> C getConfiguration(final Class<C> clazz) {
		checkClosed();
		throw new IllegalArgumentException();
	}

	@Override
	public <T> T invoke(final K key, final EntryProcessor<K, V, T> entryProcessor, final Object... arguments)
			throws EntryProcessorException {
		checkClosed();
		return entryProcessor.process(new CacheEntry(key, get(key)), arguments);
	}

	@Override
	public <T> Map<K, EntryProcessorResult<T>> invokeAll(final Set<? extends K> keys, final EntryProcessor<K, V, T> entryProcessor,
			final Object... arguments) {
		checkClosed();
		final Map<K, EntryProcessorResult<T>> ret = new HashMap<>();
		for(final K key : keys){
			try{
				final T entryProcessorProcessResult = invoke(key, entryProcessor, arguments);
				if(entryProcessorProcessResult!=null){
					ret.put(key, new EntryProcessorResult<T>() {
						@Override
						public T get() throws EntryProcessorException {
							return entryProcessorProcessResult;
						}
					});
				}
			}
			catch(final EntryProcessorException e){
				ret.put(key, new EntryProcessorResult<T>() {
					@Override
					public T get() throws EntryProcessorException {
						throw e;
					}
				});
			}
		}
		return ret;
	}

	@Override
	public String getName() {
		return springCache.getName();
	}

	@Override
	public CacheManager getCacheManager() {
		return cacheManager;
	}

	@Override
	public void close() {
		closed.set(true);
		cacheManager.destroyCache(this.getName());
	}

	@Override
	public boolean isClosed() {
		return closed.get();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T unwrap(final Class<T> clazz) {
		checkClosed();
		if(clazz.isInstance(springCache)){
			return (T) springCache;
		}
		if(springCache.getNativeCache()!=null && clazz.isInstance(springCache.getNativeCache())){
			return (T) springCache.getNativeCache();
		}
		throw new IllegalArgumentException();
	}

	@Override
	public void registerCacheEntryListener(final CacheEntryListenerConfiguration<K, V> cacheEntryListenerConfiguration) {
		checkClosed();
		throw new UnsupportedOperationException();
	}

	@Override
	public void deregisterCacheEntryListener(final CacheEntryListenerConfiguration<K, V> cacheEntryListenerConfiguration) {
		checkClosed();
		throw new UnsupportedOperationException();
	}

	@Override
	public Iterator<javax.cache.Cache.Entry<K, V>> iterator() {
		checkClosed();
		throw new UnsupportedOperationException();
	}

}

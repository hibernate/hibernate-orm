/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.resource.jdbc.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import org.hibernate.HibernateException;

/**
 * Internal collection with semantics tailored for its use.
 * <p>
 * This is essentially an HashMap wrapper, but attempting to lazily
 * initialize the HashMap based on the domain knowledge that in most
 * cases (pretty much all cases but there's occasional, rare exceptions)
 * we need to store a single entry; the HashMap and Entry allocations
 * have been shown to be wasteful in practice with both Techempower
 * and SpecJ Enterprise benchmarks.
 *
 * @author Sanne Grinovero
 */
final class LikelySingletonMap<K,V> {

	private K firstRegisteredKey = null;
	private V firstRegisteredValue = null;
	private HashMap<K, V> xref;

	public void putFailOnExisting(final K key, final V value, final String failureMessage) {
		if ( firstRegisteredKey == null ) {
			firstRegisteredKey = key;
			firstRegisteredValue = value;
		}
		else if ( key.equals( firstRegisteredKey ) ) {
			throw new HibernateException( failureMessage );
		}
		else {
			final V existing = writeOnXref().putIfAbsent( key, value );
			if ( existing != null ) {
				throw new HibernateException( failureMessage );
			}
		}
	}

	private HashMap<K, V> writeOnXref() {
		HashMap<K, V> local = this.xref;
		if ( local == null ) {
			local = this.xref = new HashMap<>();
		}
		return local;
	}

	public V remove(final K key) {
		final HashMap<K, V> map = this.xref;
		if ( key.equals( firstRegisteredKey ) ) {
			final V toReturn = this.firstRegisteredValue;
			if ( map != null && ! map.isEmpty() ) {
				final Map.Entry<K, V> firstEntry = map.entrySet().iterator().next();
				this.firstRegisteredKey = firstEntry.getKey();
				this.firstRegisteredValue = firstEntry.getValue();
				map.remove( firstEntry.getKey() );
			}
			else {
				this.firstRegisteredKey = null;
				this.firstRegisteredValue = null;
			}
			return toReturn;
		}
		else if ( map != null ) {
			return map.remove( key );
		}
		else {
			return null;
		}
	}

	public V get(final K key) {
		if ( key.equals( this.firstRegisteredKey ) ) {
			return this.firstRegisteredValue;
		}
		else {
			final HashMap<K, V> map = this.xref;
			if ( map == null ) {
				return null;
			}
			else {
				return map.get( key );
			}
		}
	}

	public void put(final K key, final V value) {
		if ( this.firstRegisteredKey == null || key.equals( this.firstRegisteredKey ) ) {
			this.firstRegisteredKey = key;
			this.firstRegisteredValue = value;
		}
		else {
			writeOnXref().put( key, value );
		}
	}

	public void clear() {
		this.firstRegisteredKey = null;
		this.firstRegisteredValue = null;
		this.xref = null; //preferred over clearing the hashmap as [this] is unlikely to be used again after a clear().
	}

	public boolean isEmpty() {
		//Rely on the invariant that the map will be empty if there is no "first registered key".
		return this.firstRegisteredKey == null;
	}

	public void forEach(BiConsumer<? super K, ? super V> action) {
		if ( this.firstRegisteredKey != null ) {
			action.accept( this.firstRegisteredKey, this.firstRegisteredValue );
			if ( this.xref != null ) {
				this.xref.forEach( action );
			}
		}
	}

}

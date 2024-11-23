/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal.util.collections;

import java.util.Arrays;
import java.util.function.Function;

/**
 * This is an internal data structure designed for very specific needs;
 * it will most often be used as a replacement for EnumMap, although
 * the focus on the Enum aspect is modelled as an int primitive:
 * think of using the ordinals of an Enum to simulate the EnumMap.
 * Proper abstraction of the indexing strategy is left to subclasses.
 * <p/>
 * There are various reasons to not expose the Enum on this class API;
 * the primary is that in some of the cases in which we need the pattern,
 * there is the need to hold an additional couple of values beyond the
 * ones modelled by the Enum, essentially having some extra keys in the map;
 * this could be modelled by defining a new Enum but that's also not ideal.
 * <p/>
 * Another reason is that the goal of this class is to allow the host to
 * save memory, as we typically need to keep references to many of these
 * objects for a long time; being able to refer purely to an array is
 * less practical but gets us benefits in memory layout and total retained
 * memory.
 *
 * @param <K> the types used to model the key
 * @param <V> the types used to model the value
 */
public abstract class LazyIndexedMap<K,V> {

	private volatile Object[] values;
	private static final Object NOT_INITIALIZED = new Object();

	protected LazyIndexedMap(final int size) {
		final Object[] vs = new Object[size];
		//could rely on null, then then we should ensure that the valueGenerator function never returns null.
		//Seems safer to guard with a custom token: the memory impact is negligible since it's a constant,
		//and we're not needing to create these objects frequently.
		Arrays.fill( vs, NOT_INITIALIZED );
		this.values = vs;
	}

	/**
	 * Both the index and the original key are requested for efficiency reasons.
	 * It is the responsibility of the caller to ensure there is a 1:1 matching relation between them.
	 * @param index storage index in the array
	 * @param originalKey the original key object, used to efficiently pass it into the valueGenerator function
	 * @param valueGenerator if no value was generated before for this index, then the valueGenerator is invoked to
	 * associate a new value and store it into the internal array at the provided index.
	 * @return the associated value to this index/key.
	 */
	protected <K1 extends K> V computeIfAbsent(final int index, final K1 originalKey, final Function<K1,V> valueGenerator) {
		final Object value = this.values[index];
		if ( value != NOT_INITIALIZED ) {
			return (V) value;
		}
		else {
			return lockedComputeIfAbsent( index, originalKey, valueGenerator );
		}
	}

	private synchronized <K1 extends K> V lockedComputeIfAbsent(final int index, final K1 originalKey, final Function<K1,V> valueGenerator) {
		//Get a fresh copy from the volatile read, while holding the global pessimistic lock in this:
		final Object[] values = this.values;
		final Object value = values[index];
		//Check again
		if ( value != NOT_INITIALIZED ) {
			return (V) value;
		}
		else {
			//Actually need to generate the value
			final V generated = valueGenerator.apply( originalKey );
			values[index] = generated;
			//re-write on the volatile reference to publish any changes to the array
			this.values = values;
			return generated;
		}
	}
}

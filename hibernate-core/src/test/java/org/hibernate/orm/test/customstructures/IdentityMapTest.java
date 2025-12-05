/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.customstructures;

import org.hibernate.internal.util.collections.IdentityMap;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

public class IdentityMapTest {

	@Test
	public void basicIdentityMapFunctionality() {
		final IdentityMap<Holder, Object> map = IdentityMap.instantiateSequenced( 10 );
		Holder k1 = new Holder( "k", 1 );
		Holder s2 = new Holder( "s", 2 );
		map.put( k1, "k1" );
		map.put( s2, "s2" );
		map.put( k1, "K1!" );
		assertThat( map ).hasSize( 2 );
		k1.name = "p";
		assertThat( map ).hasSize( 2 );
		assertThat( map.get( k1 ) ).isEqualTo( "K1!" );
		Holder k1similar = new Holder( "p", 1 );
		map.put( k1similar, "notk1" );
		assertThat( map.get( k1 ) ).isEqualTo( "K1!" );

		IdentityMap.onEachKey( map, k -> k.value = 10 );

		final Iterator<Holder> keyIterator = map.keyIterator();
		int count = 0;
		while ( keyIterator.hasNext() ) {
			final Holder key = keyIterator.next();
			assertThat( key ).isNotNull();
			count++;
			assertThat( key.value ).isEqualTo( 10 );
		}
		assertThat( count ).isEqualTo( 3 );
	}

	private static class Holder {

		//Evil: mutable keys!
		private String name;
		private int value;

		public Holder(String name, int value) {
			this.name = name;
			this.value = value;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			Holder holder = (Holder) o;
			return value == holder.value &&
				name.equals( holder.name );
		}

		@Override
		public int hashCode() {
			return Objects.hash( name, value );
		}
	}
}

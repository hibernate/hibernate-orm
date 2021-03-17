/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.customstructures;

import java.util.Iterator;
import java.util.Objects;

import org.hibernate.internal.util.collections.IdentityMap;

import org.junit.Assert;
import org.junit.Test;

public class IdentityMapTest {

    @Test
    public void basicIdentityMapFunctionality() {
        final IdentityMap<Holder, Object> map = IdentityMap.instantiateSequenced( 10 );
        Holder k1 = new Holder( "k", 1 );
        Holder s2 = new Holder( "s", 2 );
        map.put( k1, "k1"  );
        map.put( s2, "s2" );
        map.put( k1, "K1!" );
        Assert.assertEquals( 2, map.size() );
        k1.name = "p";
        Assert.assertEquals( 2, map.size() );
        Assert.assertEquals( "K1!", map.get( k1 ) );
        Holder k1similar = new Holder( "p", 1 );
        map.put( k1similar, "notk1" );
        Assert.assertEquals( "K1!", map.get( k1 ) );

        IdentityMap.onEachKey( map, k -> k.value = 10 );

        final Iterator<Holder> keyIterator = map.keyIterator();
        int count = 0;
        while ( keyIterator.hasNext() ) {
            final Holder key = keyIterator.next();
            Assert.assertNotNull( key );
            count++;
            Assert.assertEquals( 10, key.value );
        }
        Assert.assertEquals( 3, count );
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

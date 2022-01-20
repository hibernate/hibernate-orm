/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.type.java;

import org.hibernate.type.descriptor.java.BooleanJavaType;

import org.junit.Test;

import static org.junit.Assert.*;

public class BooleanJavaTypeDescriptorTest {
    private BooleanJavaType underTest = new BooleanJavaType();

    @Test
    public void testWrapShouldReturnTrueWhenYStringGiven() {
        // given
        // when
        Boolean result = underTest.wrap("Y", null);
        // then
        assertTrue(result);
    }

    @Test
    public void testWrapShouldReturnFalseWhenFStringGiven() {
        // given
        // when
        Boolean result = underTest.wrap("N", null);
        // then
        assertFalse(result);
    }

    @Test
    public void testWrapShouldReturnFalseWhenRandomStringGiven() {
        // given
        // when
        Boolean result = underTest.wrap("k", null);
        // then
        assertFalse(result);
    }

    @Test
    public void testWrapShouldReturnNullWhenNullStringGiven() {
        // given
        // when
        Boolean result = underTest.wrap(null, null);
        // then
        assertNull(result);
    }

    @Test
    public void testWrapShouldReturnFalseWhenEmptyStringGiven() {
        // given
        // when
        Boolean result = underTest.wrap("", null);
        // then
        assertFalse(result);
    }
}
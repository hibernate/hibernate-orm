/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.export.hbm;

import org.hibernate.mapping.Any;
import org.hibernate.mapping.Array;
import org.hibernate.mapping.Bag;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.DependantValue;
import org.hibernate.mapping.IdentifierBag;
import org.hibernate.mapping.JoinedSubclass;
import org.hibernate.mapping.List;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.Map;
import org.hibernate.mapping.OneToMany;
import org.hibernate.mapping.OneToOne;
import org.hibernate.mapping.PrimitiveArray;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.Set;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.SingleTableSubclass;
import org.hibernate.mapping.Subclass;
import org.hibernate.mapping.UnionSubclass;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class HBMTagVisitorTest {

	// --- HBMTagForValueVisitor ---

	@Test
	public void testManyToOneTag() {
		assertEquals("many-to-one", HBMTagForValueVisitor.INSTANCE.accept((ManyToOne) null));
	}

	@Test
	public void testOneToOneTag() {
		assertEquals("one-to-one", HBMTagForValueVisitor.INSTANCE.accept((OneToOne) null));
	}

	@Test
	public void testBagTag() {
		assertEquals("bag", HBMTagForValueVisitor.INSTANCE.accept((Bag) null));
	}

	@Test
	public void testIdentifierBagTag() {
		assertEquals("idbag", HBMTagForValueVisitor.INSTANCE.accept((IdentifierBag) null));
	}

	@Test
	public void testListTag() {
		assertEquals("list", HBMTagForValueVisitor.INSTANCE.accept((List) null));
	}

	@Test
	public void testMapTag() {
		assertEquals("map", HBMTagForValueVisitor.INSTANCE.accept((Map) null));
	}

	@Test
	public void testOneToManyTag() {
		assertEquals("one-to-many", HBMTagForValueVisitor.INSTANCE.accept((OneToMany) null));
	}

	@Test
	public void testSetTag() {
		assertEquals("set", HBMTagForValueVisitor.INSTANCE.accept((Set) null));
	}

	@Test
	public void testAnyTag() {
		assertEquals("any", HBMTagForValueVisitor.INSTANCE.accept((Any) null));
	}

	@Test
	public void testSimpleValueTag() {
		assertEquals("property", HBMTagForValueVisitor.INSTANCE.accept((SimpleValue) null));
	}

	@Test
	public void testBasicValueTag() {
		assertEquals("property", HBMTagForValueVisitor.INSTANCE.accept((BasicValue) null));
	}

	@Test
	public void testPrimitiveArrayTag() {
		assertEquals("primitive-array", HBMTagForValueVisitor.INSTANCE.accept((PrimitiveArray) null));
	}

	@Test
	public void testArrayTag() {
		assertEquals("array", HBMTagForValueVisitor.INSTANCE.accept((Array) null));
	}

	@Test
	public void testDependantValueThrows() {
		assertThrows(IllegalArgumentException.class,
				() -> HBMTagForValueVisitor.INSTANCE.accept((DependantValue) null));
	}

	// --- HBMTagForPersistentClassVisitor ---

	@Test
	public void testRootClassTag() {
		assertEquals("class", HBMTagForPersistentClassVisitor.INSTANCE.accept((RootClass) null));
	}

	@Test
	public void testUnionSubclassTag() {
		assertEquals("union-subclass", HBMTagForPersistentClassVisitor.INSTANCE.accept((UnionSubclass) null));
	}

	@Test
	public void testSingleTableSubclassTag() {
		assertEquals("subclass", HBMTagForPersistentClassVisitor.INSTANCE.accept((SingleTableSubclass) null));
	}

	@Test
	public void testJoinedSubclassTag() {
		assertEquals("joined-subclass", HBMTagForPersistentClassVisitor.INSTANCE.accept((JoinedSubclass) null));
	}

	@Test
	public void testSubclassTag() {
		assertEquals("subclass", HBMTagForPersistentClassVisitor.INSTANCE.accept((Subclass) null));
	}

	@Test
	public void testPersistentClassVisitorInstance() {
		assertNotNull(HBMTagForPersistentClassVisitor.INSTANCE);
	}

	@Test
	public void testValueVisitorInstance() {
		assertNotNull(HBMTagForValueVisitor.INSTANCE);
	}
}

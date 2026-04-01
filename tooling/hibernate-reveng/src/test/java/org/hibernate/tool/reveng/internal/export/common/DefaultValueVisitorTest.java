/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.export.common;

import org.hibernate.mapping.Any;
import org.hibernate.mapping.Array;
import org.hibernate.mapping.Bag;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.DependantValue;
import org.hibernate.mapping.IdentifierBag;
import org.hibernate.mapping.List;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.Map;
import org.hibernate.mapping.OneToMany;
import org.hibernate.mapping.OneToOne;
import org.hibernate.mapping.PrimitiveArray;
import org.hibernate.mapping.Set;
import org.hibernate.mapping.SimpleValue;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DefaultValueVisitorTest {

	@Test
	public void testThrowExceptionMode() {
		DefaultValueVisitor visitor = new DefaultValueVisitor(true) {};
		assertThrows(UnsupportedOperationException.class, () -> visitor.accept((SimpleValue) null));
	}

	@Test
	public void testNoThrowMode() {
		DefaultValueVisitor visitor = new DefaultValueVisitor(false) {};
		assertNull(visitor.accept((SimpleValue) null));
		assertNull(visitor.accept((ManyToOne) null));
		assertNull(visitor.accept((OneToOne) null));
		assertNull(visitor.accept((OneToMany) null));
		assertNull(visitor.accept((Component) null));
		assertNull(visitor.accept((DependantValue) null));
		assertNull(visitor.accept((Any) null));
		assertNull(visitor.accept((Bag) null));
		assertNull(visitor.accept((IdentifierBag) null));
		assertNull(visitor.accept((List) null));
		assertNull(visitor.accept((PrimitiveArray) null));
		assertNull(visitor.accept((Array) null));
		assertNull(visitor.accept((Map) null));
		assertNull(visitor.accept((Set) null));
		assertNull(visitor.accept((BasicValue) null));
	}
}

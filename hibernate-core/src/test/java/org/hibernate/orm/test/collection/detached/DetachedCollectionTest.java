/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collection.detached;

import org.hibernate.Hibernate;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@DomainModel(
	annotatedClasses = {One.class, Many.class, Several.class}
)
@SessionFactory
public class DetachedCollectionTest {

	@Test
	public void testToOne(SessionFactoryScope scope) {
		One one = new One();
		scope.inTransaction( session -> session.persist(one) );
		One two = Hibernate.createDetachedProxy( scope.getSessionFactory(), One.class, one.id );
		assertEquals( one.id, two.getId() );
		Many many = new Many();
		many.one = two;
		scope.inTransaction( session -> session.persist(many) );
		scope.inTransaction( session -> {
			Many m = session.find(Many.class, many.id);
			assertNotNull(m.one);
			assertEquals( one.id, m.one.id );
		});
	}

	@Test
	public void testOwned(SessionFactoryScope scope) {
		Several several = new Several();
		several.many = new HashSet<>();
		Many many = new Many();
		several.many.add(many);
		assertNotNull(several.many);
		scope.inTransaction(session -> {
			session.persist(several);
			session.persist(many);
		});
		assertNotNull(several.many);
		several.many = Hibernate.<Many>set().createDetachedInstance();
		assertFalse(Hibernate.isInitialized(several.many));
		scope.inTransaction(session -> {
			Several merged = (Several) session.merge(several);
			assertNotNull(merged.many);
			assertTrue(Hibernate.isInitialized(merged.many));
			assertEquals(1, merged.many.size());
		});
		scope.inTransaction(session -> {
			Several found = session.find(Several.class, several.id);
			Hibernate.initialize(found.many);
			assertNotNull(found.many);
			assertEquals(1, found.many.size());
		});
	}

	@Test
	public void testUnowned(SessionFactoryScope scope) {
		One one = new One();
		one.many = new ArrayList<>();
		Many many = new Many();
		many.one = one;
		one.many.add(many);
		assertNotNull(one.many);
		scope.inTransaction(session -> {
			session.persist(one);
			session.persist(many);
		});
		assertNotNull(one.many);
		one.many = Hibernate.<Many>list().createDetachedInstance();
		assertFalse(Hibernate.isInitialized(one.many));
		scope.inTransaction(session -> {
			One merged = (One) session.merge(one);
			assertNotNull(merged.many);
			assertTrue(Hibernate.isInitialized(merged.many));
			assertEquals(1, merged.many.size());
		});
		scope.inTransaction(session -> {
			One found = session.find(One.class, one.id);
			Hibernate.initialize(found.many);
			assertNotNull(found.many);
			assertEquals(1, found.many.size());
		});
	}
}

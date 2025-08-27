/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hql.nofrom;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.query.SemanticException;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

@SessionFactory
@DomainModel(annotatedClasses = NoFromTest.Thing.class)
public class NoFromTest {

	@Test
	public void test(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			s.persist(new Thing());
			s.persist(new Thing());
		});
		scope.inSession( s -> {
			List<Thing> things = s.createSelectionQuery("where id = 1", Thing.class).getResultList();
			assertEquals(1, things.size());
			assertEquals(1, things.get(0).id);
			Thing thing = s.createSelectionQuery("where id = 1", Thing.class).getSingleResultOrNull();
			assertNotNull(thing);
			assertEquals(1, thing.id);
		});
		scope.inSession( s -> {
			List<Thing> things = s.createSelectionQuery("order by id desc", Thing.class).getResultList();
			assertEquals(2, things.size());
			assertEquals(2, things.get(0).id);
			assertEquals(1, things.get(1).id);
		});
	}

	@Test
	public void testError(SessionFactoryScope scope) {
		scope.inSession( s -> {
			try {
				s.createSelectionQuery("where id = 1").getResultList();
				fail();
			}
			catch (SemanticException se) {}
		});
		scope.inSession( s -> {
			try {
				s.createSelectionQuery("order by id desc").getResultList();
				fail();
			}
			catch (SemanticException se) {}
		});
	}

	@Entity(name = "Thing")
	static class Thing {
		@Id @GeneratedValue Long id;
	}
}

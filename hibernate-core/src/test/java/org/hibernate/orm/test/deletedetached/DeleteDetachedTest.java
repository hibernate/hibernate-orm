/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.deletedetached;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.Version;
import org.hibernate.StaleObjectStateException;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@SessionFactory
@DomainModel(annotatedClasses = DeleteDetachedTest.Thing.class)
@JiraKey("HHH-18553")
public class DeleteDetachedTest {
	@Test void testCanRemove(SessionFactoryScope scope) {
		Thing thing = new Thing();
		thing.stuff = "Some stuff about the thing";
		scope.inTransaction(s -> s.persist(thing));
		scope.inTransaction(s -> {
			Thing otherThing = s.find(Thing.class, thing.id);
			assertNotNull(otherThing);
			s.remove(thing);
			assertFalse(s.contains(thing));
			assertFalse(s.contains(otherThing));
		});
		scope.inTransaction(s -> assertNull(s.find(Thing.class, thing.id)));
	}
	@Test void testCantRemove(SessionFactoryScope scope) {
		Thing thing = new Thing();
		thing.stuff = "Some stuff about the thing";
		scope.inTransaction(s -> s.persist(thing));
		scope.inTransaction(s -> {
			Thing otherThing = s.find(Thing.class, thing.id);
			otherThing.stuff = "Other different stuff about the thing";
			assertNotNull(otherThing);
			try {
				s.remove(thing);
				fail("Should have failed because detached object is stale");
			}
			catch (OptimisticLockException exception) {
				// expected
				assertTrue( exception.getCause() instanceof StaleObjectStateException );
			}
			assertFalse(s.contains(thing));
			assertTrue(s.contains(otherThing));
		});
		scope.inTransaction(s -> assertNotNull(s.find(Thing.class, thing.id)));
	}
	@Test void testAlreadyRemoved(SessionFactoryScope scope) {
		Thing thing = new Thing();
		thing.stuff = "Some stuff about the thing";
		scope.inTransaction(s -> s.persist(thing));
		scope.inTransaction(s -> {
			Thing otherThing = s.find(Thing.class, thing.id);
			assertNotNull(otherThing);
			s.remove(otherThing);
			s.remove(thing);
			assertFalse(s.contains(thing));
			assertFalse(s.contains(otherThing));
		});
		scope.inTransaction(s -> assertNull(s.find(Thing.class, thing.id)));
	}
	@Entity
	static class Thing {
		@GeneratedValue @Id long id;
		@Version int version;
		String stuff;
	}
}

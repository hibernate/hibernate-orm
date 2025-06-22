/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.deletedetached;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.PropertyValueException;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SessionFactory
@DomainModel(annotatedClasses = DeleteDetachedOptionalityViolationTest.Thing.class)
@JiraKey("HHH-2792")
public class DeleteDetachedOptionalityViolationTest {
	@Test
	void testRemoveDetachedWithNull(SessionFactoryScope scope) {
		Thing thing = new Thing();
		thing.stuff = "Some stuff about the thing";
		scope.inTransaction( s -> s.persist( thing ) );
		scope.inTransaction( s -> {
			Thing detachedThing = new Thing();
			detachedThing.id = thing.id;
			assertThrows( PropertyValueException.class,
					() -> s.remove( detachedThing ),
					"not-null property references a null or transient value: " + Thing.class.getName() + ".stuff"
			);
		} );
	}

	@Test
	void testRemoveDetachedProxy(SessionFactoryScope scope) {
		Thing thing = new Thing();
		thing.stuff = "Some stuff about the thing";
		scope.inTransaction( s -> s.persist( thing ) );
		Thing detachedThing = scope.fromTransaction( s -> s.getReference( Thing.class, thing.id ) );
		scope.inTransaction( s -> {
			s.remove( detachedThing );
		} );
		scope.inTransaction( s -> assertNull( s.find( Thing.class, thing.id ) ) );
	}

	@Entity
	static class Thing {
		@GeneratedValue
		@Id
		long id;
		@Basic(optional = false)
		String stuff;
	}
}

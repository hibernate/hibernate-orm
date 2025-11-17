/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.actionqueue;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import org.hibernate.cfg.BatchSettings;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

@Jpa(annotatedClasses =
		{CollectionUpdateOrderingTest.Thing.class,
		CollectionUpdateOrderingTest.OtherThing.class},
		integrationSettings = @Setting(name = BatchSettings.ORDER_UPDATES, value = "true"))
class CollectionUpdateOrderingTest {

	@Test
	void test(EntityManagerFactoryScope scope) {
		scope.inTransaction( session -> {
			Thing thing1 = new Thing();
			Thing thing2 = new Thing();
			thing2.id = 2;
			thing1.id = 3; //bigger
			OtherThing otherThing1 = new OtherThing();
			OtherThing otherThing2 = new OtherThing();
			otherThing1.id = 1;
			otherThing2.id = 2;
			thing1.otherThings.add(otherThing1);
			thing1.otherThings.add(otherThing2);
			session.persist( thing1 );
			session.persist( thing2 );
		} );
		scope.inTransaction( session -> {
			Thing thing2 = session.find(Thing.class, 2L);
			Thing thing1 = session.find(Thing.class, 3L);
			OtherThing otherThing = thing1.otherThings.iterator().next();
			thing1.otherThings.remove(otherThing);
			thing2.otherThings.add(otherThing);
		});
	}

	@Entity(name = "Thing")
	static class Thing {
		@Id long id;
		@OneToMany(cascade = CascadeType.PERSIST)
		Set<OtherThing> otherThings = new HashSet<>();
	}

	@Entity(name = "OtherThing")
	static class OtherThing {
		@Id long id;
	}
}

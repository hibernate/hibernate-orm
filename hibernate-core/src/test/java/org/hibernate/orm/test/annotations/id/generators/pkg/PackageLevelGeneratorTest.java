/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.id.generators.pkg;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SessionFactory
@DomainModel(annotatedClasses = {
		PackageLevelGeneratorTest.EntityWithDefaultedPackageGenerator.class,
		PackageLevelGeneratorTest.EntityWithDefaultedPackageGenerator2.class,
		PackageLevelGeneratorTest.EntityWithDefaultedPackageGenerator3.class,
		PackageLevelGeneratorTest.EntityWithDefaultedPackageGenerator4.class
})
public class PackageLevelGeneratorTest {
	@Test
	void testAnonGenerator(SessionFactoryScope scope) {
		scope.inSession(s-> {
			EntityWithDefaultedPackageGenerator entity1 = new EntityWithDefaultedPackageGenerator();
			EntityWithDefaultedPackageGenerator2 entity2 = new EntityWithDefaultedPackageGenerator2();
			EntityWithDefaultedPackageGenerator3 entity3 = new EntityWithDefaultedPackageGenerator3();
			EntityWithDefaultedPackageGenerator4 entity4 = new EntityWithDefaultedPackageGenerator4();
			s.persist(entity1);
			s.persist(entity2);
			s.persist(entity3);
			s.persist(entity4);
			assertEquals(42, entity1.id);
			assertEquals(42, entity2.id);
			assertEquals(42, entity3.id);
			assertEquals(70, entity4.id);
		});
	}
	@Entity(name="EntityWithDefaultedPackageGenerator")
	static class EntityWithDefaultedPackageGenerator {
		@Id
		@GeneratedValue
		long id;
	}
	@Entity(name = "EntityWithDefaultedPackageGenerator2")
	static class EntityWithDefaultedPackageGenerator2 {
		@Id
		@GeneratedValue
		long id;
	}
	@Entity(name = "EntityWithDefaultedPackageGenerator3")
	static class EntityWithDefaultedPackageGenerator3 {
		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE)
		long id;
	}
	@Entity(name = "EntityWithDefaultedPackageGenerator4")
	static class EntityWithDefaultedPackageGenerator4 {
		@Id
		@GeneratedValue(strategy = GenerationType.TABLE)
		long id;
	}
}

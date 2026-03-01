/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.cascade.persist;

import jakarta.persistence.Basic;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import org.hibernate.cfg.JpaComplianceSettings;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Jpa(annotatedClasses =
		{CascadePersistJpaTest.Parent.class,
				CascadePersistJpaTest.Child.class},
		integrationSettings = {
				@Setting(name = JpaComplianceSettings.JPA_COMPLIANCE, value = "true")
		}
)
public class CascadePersistJpaTest {
	@Test void test(EntityManagerFactoryScope scope) {
		Parent p = new Parent();
		scope.inTransaction(s -> s.persist(p));
		scope.inTransaction(s -> {
			Parent parent = s.find(Parent.class, p.id);
			Child child = new Child();
			child.parent = parent;
			parent.children.add(child);
		});
		scope.inTransaction(s -> {
			Parent parent = s.find(Parent.class, p.id);
			assertEquals(1, parent.children.size());
			Child child = parent.children.iterator().next();
			s.remove(child);
			parent.children.remove(child);
		});
	}

	@Entity(name = "Parent")
	static class Parent {
		@Id
		@GeneratedValue
		Long id;

		@OneToMany(cascade = CascadeType.PERSIST,
				mappedBy = "parent")
		Set<Child> children = new HashSet<>();
	}
	@Entity(name = "Child")
	static class Child {
		@Id @GeneratedValue
		private Long id;

		@Basic(optional = false)
		String name = "child";

		@ManyToOne(optional = false)
		Parent parent;
	}
}

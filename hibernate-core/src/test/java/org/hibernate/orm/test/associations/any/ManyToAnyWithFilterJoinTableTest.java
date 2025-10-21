/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.associations.any;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.Table;
import org.hibernate.annotations.AnyKeyJavaClass;
import org.hibernate.annotations.FilterJoinTable;
import org.hibernate.annotations.ManyToAny;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;


/**
 * Tests that it is possible to add a @FilterJoinTable to a @ManyToAny association without referencing an @Entity.
 *
 * @author Vincent Bouthinon
 */
@Jpa(
		annotatedClasses = {
				ManyToAnyWithFilterJoinTableTest.Actor.class
		},
		integrationSettings = @Setting(name = JdbcSettings.SHOW_SQL, value = "true")
)
@JiraKey("HHH-18986")
class ManyToAnyWithFilterJoinTableTest {

	@Test
	void test(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					// Error reported :
					// Collection 'org.hibernate.orm.test.associations.any.ManyToAnyWithFilterJoinTableTest$Actor.contacts' is an association with no join table and may not have a '@FilterJoinTable'
				}
		);
	}

	@Entity(name = "actor")
	@Table(name = "TACTOR")
	public static class Actor {

		@Id
		@GeneratedValue
		private Long id;

		@ManyToAny
		@AnyKeyJavaClass(Long.class)
		@Column(name = "ROLE")
		@JoinTable(joinColumns = @JoinColumn(name = "SOURCE"), inverseJoinColumns = @JoinColumn(name = "DEST"))
		@FilterJoinTable(name = "confidentialite", condition = "1=1")
		private Set<IContact> contacts = new HashSet<>();
	}

	public interface IContact {
	}
}

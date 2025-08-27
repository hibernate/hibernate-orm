/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.manytoone;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;
import org.hibernate.annotations.Any;
import org.hibernate.annotations.AnyKeyJavaClass;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static jakarta.persistence.FetchType.LAZY;

/**
 * Allows testing a @OneToMany mappedBy relationship with a @Any as the return variable and persist in secondary table.
 *
 * @author Vincent Bouthinon
 */
@Jpa(
		annotatedClasses = {
				ManyToOneWithAnyAndSecondaryTable.Actor.class,
				ManyToOneWithAnyAndSecondaryTable.Contact.class
		},
		integrationSettings = @Setting(name = JdbcSettings.SHOW_SQL, value = "true")
)
@JiraKey("HHH-18750")
class ManyToOneWithAnyAndSecondaryTable {

	@Test
	void testMappingManyToOneMappedByAnyPersistedInSecondaryTable(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Actor actor = new Actor();
					actor.addToContacts( new Contact() );
					entityManager.persist( actor );
					entityManager.flush();
					entityManager.clear();

					List<Actor> actors = entityManager.createQuery( "select a from actor a", Actor.class )
							.getResultList();
					Assertions.assertEquals( actors.size(), 2 );
				}
		);
	}


	@Entity(name = "actor")
	@Table(name = "TACTOR")
	public static class Actor {

		@Id
		@GeneratedValue
		private Long id;

		@OneToMany(mappedBy = "objetMaitre", cascade = {CascadeType.ALL})
		@Fetch(FetchMode.SELECT)
		private Set<Contact> contacts = new HashSet<>();

		public Set<Contact> getContacts() {
			return contacts;
		}

		public void setContacts(Set<Contact> contacts) {
			this.contacts = contacts;
		}

		public void addToContacts(Contact contact) {
			this.contacts.add( contact );
			contact.setObjetMaitre( this );
		}
	}

	@Entity(name = "contact")
	@SecondaryTable(name = "TPERSONNEPHYSIQUE")
	public static class Contact extends Actor {
		@Id
		@GeneratedValue
		private Long id;

		@Any(fetch = LAZY)
		@AnyKeyJavaClass(Long.class)
		@JoinColumn(name = "OBJETMAITRE_ID", table = "TPERSONNEPHYSIQUE")
		@Column(name = "OBJETMAITRE_ROLE")
		private Actor objetMaitre;

		public Actor getObjetMaitre() {
			return objetMaitre;
		}

		public void setObjetMaitre(Actor objetMaitre) {
			this.objetMaitre = objetMaitre;
		}
	}
}

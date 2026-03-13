/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.xml;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
@Jpa(
		xmlMappings = {
				"org/hibernate/orm/test/jpa/xml/orm4.xml"
		},
		annotatedClasses = {DelimitedIdentifiersTest.Item.class}
//		integrationSettings = { @Setting(name = AvailableSettings.GLOBALLY_QUOTED_IDENTIFIERS, value = "true") }
)
public class DelimitedIdentifiersTest {

	@Test
	public void testXmlDelimitedIdentifiers(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Item item = new Item();
			item.setName( "John" );
			entityManager.persist( item );
		} );
		scope.inTransaction( entityManager -> {
			final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
			final CriteriaQuery<Item> query = criteriaBuilder.createQuery(Item.class);
			final Root<Item> from = query.from(Item.class);
			query.select(from);
			List<Item> items = entityManager.createQuery( query ).getResultList();
			assertEquals(1, items.size());
		} );
	}

	@Entity
	public static class Item {

		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE)
		private int id;

		private String name;

		public Item() {
			name = "";
		}

		public int getId() {
			return id;
		}

		/**
		 * Returns the item name.
		 *
		 * @return not <code>null</code>.
		 */
		public String getName() {
			return name;
		}

		/**
		 * Sets the item name.
		 *
		 * @param name <code>null</code> strings are converted to empty strings.
		 */
		public void setName(String name) {
			this.name = name == null ? "" : name;
		}
	}

}

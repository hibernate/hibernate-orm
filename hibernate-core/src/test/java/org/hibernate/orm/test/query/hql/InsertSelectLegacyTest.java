/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.id.enhanced.LegacyNamingStrategy;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DomainModel(
		annotatedClasses = {
				InsertSelectLegacyTest.Person.class,
				InsertSelectLegacyTest.Document.class
		}
)
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.SHOW_SQL, value = "true"),
				@Setting(name = AvailableSettings.FORMAT_SQL, value = "true"),
				@Setting(name = AvailableSettings.ID_DB_STRUCTURE_NAMING_STRATEGY,
						value = LegacyNamingStrategy.STRATEGY_NAME)
		}
)
@SessionFactory
public class InsertSelectLegacyTest {

	@Test
	@JiraKey(value = "HHH-18835")
	void testInsertSelect(SessionFactoryScope scope) {

		scope.inTransaction( session -> {
			Person person = new Person();
			person.name = "Peter";
			session.persist( person );

			session.createMutationQuery(
							"insert into Document(name,owner) select concat(p.name,'s document'), p from Person p" )
					.executeUpdate();

			Document document = session.createQuery( "select d from Document d", Document.class ).getSingleResult();
			assertNotNull( document );
			assertEquals( "Peters document", document.name );
		} );
	}

	@Entity(name = "Person")
	public static class Person {

		@Id
		@GeneratedValue
		Long id;

		String name;
	}

	@Entity(name = "Document")
	public static class Document {

		@Id
		@GeneratedValue
		Long id;

		String name;

		@ManyToOne
		Person owner;
	}
}

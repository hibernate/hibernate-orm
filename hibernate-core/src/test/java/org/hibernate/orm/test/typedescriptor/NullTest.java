/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.typedescriptor;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Yanming Zhou
 */
@DomainModel(
		annotatedClasses = NullTest.SimpleEntity.class
)
@SessionFactory
public class NullTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> session.persist( new SimpleEntity() )
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session ->
						session.createMutationQuery( "delete from SimpleEntity" ).executeUpdate()
		);
	}

	@Test
	@JiraKey("HHH-18581")
	public void passingNullAsParameterOfNativeQuery(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					SimpleEntity persisted = session.createNativeQuery(
							"select * from SimpleEntity where name is null or name=:name",
							SimpleEntity.class
					).setParameter( "name", null ).uniqueResult();

					assertNotNull( persisted );
				}
		);
	}

	@Test
	public void passingNullAsParameterOfQuery(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					SimpleEntity persisted = session.createQuery(
							"from SimpleEntity where name is null or name=:name",
							SimpleEntity.class
					).setParameter( "name", null ).uniqueResult();

					assertNotNull( persisted );
				}
		);
	}

	@Entity(name = "SimpleEntity")
	static class SimpleEntity {
		@Id
		@GeneratedValue
		private Integer id;

		private String name;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}

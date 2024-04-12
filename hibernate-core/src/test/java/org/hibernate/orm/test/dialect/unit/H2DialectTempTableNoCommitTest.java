/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.dialect.unit;

import org.hibernate.dialect.H2Dialect;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Jan Schatteman
 */
@DomainModel(
		annotatedClasses = {
				H2DialectTempTableNoCommitTest.Person.class,
				H2DialectTempTableNoCommitTest.Engineer.class,
				H2DialectTempTableNoCommitTest.Doctor.class
		}
)
@SessionFactory
@RequiresDialect( H2Dialect.class )
@JiraKey( "HHH-17943" )
public class H2DialectTempTableNoCommitTest {

	@Test
	public void noCommitAfterTempTableCreationTest(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					// the following shouldn't commit anything to the DB
					Doctor d = new Doctor();
					d.setId( 2L );
					session.persist( d );
					session.flush();
					session.createMutationQuery( "update Engineer set fellow = false where fellow = true" ).executeUpdate();
					session.getTransaction().markRollbackOnly();
				}
		);
		scope.inTransaction(
				session -> {
					assertNull(session.find( Doctor.class, 2L ));
				}
		);
	}

	@Entity(name = "Person")
	@Inheritance(strategy = InheritanceType.JOINED)
	public static class Person {

		@Id
		private Long id;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}
	}

	@Entity(name = "Engineer")
	public static class Engineer extends Person {

		private boolean fellow;

		public boolean isFellow() {
			return fellow;
		}

		public void setFellow(boolean fellow) {
			this.fellow = fellow;
		}
	}

	@Entity(name = "Doctor")
	public static class Doctor {
		@Id
		private Long id;
		private String name;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
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

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.dialect.unit;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.query.sqm.mutation.internal.temptable.LocalTemporaryTableStrategy;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Jan Schatteman
 */
@DomainModel(
		annotatedClasses = {
				DialectTempTableNoCommitTest.Person.class,
				DialectTempTableNoCommitTest.Engineer.class,
				DialectTempTableNoCommitTest.Doctor.class
		}
)
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsTemporaryTable.class)
@JiraKey("HHH-17943")
public class DialectTempTableNoCommitTest {

	private static final Long ID = 1l;

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session ->
						session.createMutationQuery( "delete from Doctor" ).executeUpdate()
		);
	}

	@Test
	@ServiceRegistry(
			settings = @Setting(name = LocalTemporaryTableStrategy.DROP_ID_TABLES, value = "true")
	)
//	@SkipForDialect(dialectClass = H2Dialect.class)
	@SessionFactory
	public void noCommitAfterTempTableCreationAndDropTempTableTest(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					// the following shouldn't commit anything to the DB
					Doctor d = new Doctor();
					d.setId( ID );
					session.persist( d );
					session.flush();
					session.createMutationQuery( "update Engineer set fellow = false where fellow = true" )
							.executeUpdate();
					session.getTransaction().markRollbackOnly();
				}
		);
		scope.inTransaction(
				session -> {
					assertNull( session.find( Doctor.class, ID ) );
				}
		);

		scope.inTransaction(
				session -> {
					// the following shouldn't commit anything to the DB
					Doctor d = new Doctor();
					d.setId( ID );
					session.persist( d );
					session.flush();
					session.createMutationQuery( "update Engineer set fellow = false where fellow = true" )
							.executeUpdate();
				}
		);
		scope.inTransaction(
				session -> {
					assertNotNull( session.find( Doctor.class, ID ) );
				}
		);
	}

	@Test
	@ServiceRegistry(
			settings = @Setting(name = LocalTemporaryTableStrategy.DROP_ID_TABLES, value = "false")
	)
	@SessionFactory
	public void noCommitAfterTempTableCreationAndNoDropTempTableTest2(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					// the following shouldn't commit anything to the DB
					Doctor d = new Doctor();
					d.setId( ID );
					session.persist( d );
					session.flush();
					session.createMutationQuery( "update Engineer set fellow = false where fellow = true" )
							.executeUpdate();
					session.getTransaction().markRollbackOnly();
				}
		);
		scope.inTransaction(
				session -> {
					assertNull( session.find( Doctor.class, ID ) );
				}
		);

		scope.inTransaction(
				session -> {
					// the following shouldn't commit anything to the DB
					Doctor d = new Doctor();
					d.setId( ID );
					session.persist( d );
					session.flush();
					session.createMutationQuery( "update Engineer set fellow = false where fellow = true" )
							.executeUpdate();
				}
		);
		scope.inTransaction(
				session -> {
					assertNotNull( session.find( Doctor.class, ID ) );
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

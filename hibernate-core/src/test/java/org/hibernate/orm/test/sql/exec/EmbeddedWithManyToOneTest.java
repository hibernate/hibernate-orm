/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql.exec;

import java.io.Serializable;
import java.util.Objects;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import org.hibernate.Hibernate;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Andrea Boriero
 */
@DomainModel(
		annotatedClasses = {
				EmbeddedWithManyToOneTest.SystemUser.class,
				EmbeddedWithManyToOneTest.Subsystem.class
		}
)
@SessionFactory(
		generateStatistics = true,
		useCollectingStatementInspector = true)
public class EmbeddedWithManyToOneTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Subsystem subsystem = new Subsystem( 2, "sub1" );
					PK userKey = new PK( subsystem, "Fab" );
					SystemUser user = new SystemUser( 1, userKey, "Fab" );

					session.persist( subsystem );
					session.persist( user );
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testGet(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				session -> {
					SystemUser systemUser = session.get( SystemUser.class, 1 );
					assertTrue( Hibernate.isInitialized( systemUser.getPk() ) );
					statementInspector.assertExecutedCount( 1 );
					statementInspector.assertNumberOfOccurrenceInQuery( 0, "join", 1 );
				}
		);
	}

	@Test
	public void testHqlSelect(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				session -> {
					SystemUser systemUser = session.createQuery( "from SystemUser", SystemUser.class )
							.uniqueResult();
					assertTrue( Hibernate.isInitialized( systemUser.getPk() ) );
					statementInspector.assertExecutedCount( 2 );
					statementInspector.assertNumberOfOccurrenceInQuery( 1, "join", 0 );
					statementInspector.assertNumberOfOccurrenceInQuery( 0, "join", 0 );
				}
		);
	}

	@Entity(name = "SystemUser")
	public static class SystemUser {
		@Id
		private Integer id;

		@Embedded
		private PK pk;

		private String name;

		public SystemUser() {
		}

		public SystemUser(Integer id, PK pk, String name) {
			this.id = id;
			this.pk = pk;
			this.name = name;
		}

		public PK getPk() {
			return pk;
		}

		public void setPk(PK pk) {
			this.pk = pk;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Embeddable
	public static class PK implements Serializable {

		@ManyToOne
		private Subsystem subsystem;

		private String username;

		public PK(Subsystem subsystem, String username) {
			this.subsystem = subsystem;
			this.username = username;
		}

		private PK() {
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			PK pk = (PK) o;
			return Objects.equals( subsystem, pk.subsystem ) &&
					Objects.equals( username, pk.username );
		}

		@Override
		public int hashCode() {
			return Objects.hash( subsystem, username );
		}
	}

	@Entity(name = "Subsystem")
	public static class Subsystem {

		@Id
		private Integer id;

		private String description;

		public Subsystem() {
		}

		public Subsystem(Integer id, String description) {
			this.id = id;
			this.description = description;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}
	}
}

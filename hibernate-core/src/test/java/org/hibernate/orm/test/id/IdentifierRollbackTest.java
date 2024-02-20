/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.id;

import org.hibernate.Transaction;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * @author Jan Schatteman
 */
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.USE_IDENTIFIER_ROLLBACK, value = "true")
		}
)
@DomainModel(annotatedClasses = { IdentifierRollbackTest.Foo.class})
@SessionFactory
public class IdentifierRollbackTest {

	@Test
	public void testOrphanedId(SessionFactoryScope scope) {
		Foo f = new Foo();
		f.setField( 1 );
		scope.inSession(
				session -> {
					Transaction t = session.beginTransaction();
					session.persist( f );
					t.rollback();
				}
		);
		scope.inTransaction(
				session -> {
					session.persist( f );
				}
		);
	}

	@Entity
	public static class Foo {
		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		private Long id;

		private int field;

		public int getField() {
			return field;
		}

		public void setField(int field) {
			this.field = field;
		}
	}
}

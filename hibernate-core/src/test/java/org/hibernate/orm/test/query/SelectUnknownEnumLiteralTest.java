/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Tuple;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DomainModel(annotatedClasses = SelectUnknownEnumLiteralTest.Transaction.class)
@SessionFactory
public class SelectUnknownEnumLiteralTest {

	@BeforeAll
	void setup(SessionFactoryScope scope) {
		scope.inTransaction( em -> em.persist( new Transaction( 1L, "abc" ) ) );
	}

	@AfterAll
	void clean(SessionFactoryScope scope) {
		scope.inTransaction( em -> em.createMutationQuery( "delete from Tx" ).executeUpdate() );
	}

	@Test
	void test(SessionFactoryScope scope) {
		final List<Tuple> tuples = scope.fromSession( em ->
				em.createQuery(
						"SELECT org.hibernate.orm.test.query.SelectUnknownEnumLiteralTest$Type.TRANSACTION, e.id, e.reference FROM Tx e",
						Tuple.class ).getResultList() );
		assertEquals( 1, tuples.size() );
		assertEquals( Type.TRANSACTION, tuples.get( 0 ).get( 0 ) );
		assertEquals( 1L, tuples.get( 0 ).get( 1 ) );
	}

	@Entity(name = "Tx")
	static class Transaction {
		@Id
		Long id;
		String reference;

		Transaction() {
		}

		Transaction(Long id, String reference) {
			this.id = id;
			this.reference = reference;
		}
	}

	enum Type {
		TRANSACTION, DIRECT_DEBIT_GROUP, DIRECT_DEBIT
	}
}

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.userguide.hql;

import org.hibernate.query.MutationQuery;
import org.hibernate.query.Query;
import org.hibernate.query.SelectionQuery;
import org.hibernate.userguide.model.Account;
import org.hibernate.userguide.model.Call;
import org.hibernate.userguide.model.CreditCardPayment;
import org.hibernate.userguide.model.Person;
import org.hibernate.userguide.model.Phone;
import org.hibernate.userguide.model.WireTransferPayment;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

/**
 * @author Steve Ebersole
 */
@DomainModel(
		annotatedClasses = {
				Person.class,
				Phone.class,
				Call.class,
				Account.class,
				CreditCardPayment.class,
				WireTransferPayment.class
		}
)
@SessionFactory
public class MutationQueryExampleTests {

	@Test
	@FailureExpected( reason = "Illegal mutation query" )
	public void queryTest(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			//tag::example-hql-mutation-query-query[]
			// cannot be validated until execution
			Query<Person> query = session.createQuery( "select p from Person p", Person.class );
			query.executeUpdate();
			//end::example-hql-mutation-query-query[]
		} );
	}

	@Test
	@FailureExpected( reason = "Illegal mutation query" )
	public void selectionQueryTest(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			//tag::example-hql-mutation-query[]
			// can be validated while creating the MutationQuery
			MutationQuery badQuery = session.createMutationQuery( "select p from Person p" );
			//end::example-hql-mutation-query[]
		} );
	}

	@Test
	@FailureExpected( reason = "Illegal mutation query" )
	public void namedSelectionQueryTest(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			//tag::example-hql-named-mutation-query[]
			// can be validated while creating the MutationQuery
			MutationQuery badQuery = session.createNamedMutationQuery( "get_person_by_name" );
			//end::example-hql-named-mutation-query[]
		} );
	}

	@Test
	@FailureExpected( reason = "Illegal mutation query" )
	public void namedQueryTest(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			//tag::example-hql-named-mutation-query-query[]
			// cannot be validated until execution
			Query query = session.createNamedQuery( "get_person_by_name" );
			query.getResultList();
			//end::example-hql-named-mutation-query-query[]
		} );
	}
}

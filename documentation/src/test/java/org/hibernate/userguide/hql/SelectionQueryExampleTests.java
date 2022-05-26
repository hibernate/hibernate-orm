/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.userguide.hql;

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
import org.hibernate.testing.orm.junit.FailureExpectedCallback;
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
public class SelectionQueryExampleTests {

	@Test
	@FailureExpected( reason = "Illegal selection query" )
	public void selectionQueryTest(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			//tag::example-hql-selection-query[]
			// can be validated while creating the SelectionQuery
			SelectionQuery<?> badQuery = session.createSelectionQuery( "delete Person" );
			//end::example-hql-selection-query[]
		} );
	}

	@FailureExpectedCallback
	public void cleanTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.createMutationQuery( "delete Person" ).executeUpdate();
		} );
	}

	@Test
	@FailureExpected( reason = "Illegal selection query" )
	public void queryTest(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			//tag::example-hql-selection-query-query[]
			// cannot be validated until execution
			Query query = session.createQuery( "delete Person" );
			query.getResultList();
			//end::example-hql-selection-query-query[]
		} );
	}

	@Test
	@FailureExpected( reason = "Illegal selection query" )
	public void namedSelectionQueryTest(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			//tag::example-hql-named-selection-query[]
			// can be validated while creating the SelectionQuery
			SelectionQuery<?> badQuery = session.getNamedQuery( "delete_Person" );
			//end::example-hql-named-selection-query[]
		} );
	}

	@Test
	@FailureExpected( reason = "Illegal selection query" )
	public void namedQueryTest(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			//tag::example-hql-named-selection-query-query[]
			// cannot be validated until execution
			Query query = session.getNamedQuery( "delete_Person" );
			query.getResultList();
			//end::example-hql-named-selection-query-query[]
		} );
	}
}

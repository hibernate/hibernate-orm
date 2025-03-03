/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hql;

import org.hibernate.testing.orm.domain.userguide.Account;
import org.hibernate.testing.orm.domain.userguide.Call;
import org.hibernate.testing.orm.domain.userguide.CreditCardPayment;
import org.hibernate.testing.orm.domain.userguide.Person;
import org.hibernate.testing.orm.domain.userguide.Phone;
import org.hibernate.testing.orm.domain.userguide.WireTransferPayment;
import org.hibernate.query.Query;
import org.hibernate.query.SelectionQuery;

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

	@Test
	@FailureExpected( reason = "Illegal selection query" )
	public void queryTest(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			//tag::example-hql-selection-query-query[]
			// cannot be validated until execution
			Query query = session.createQuery( "delete Person", Person.class );
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

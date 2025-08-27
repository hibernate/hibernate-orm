/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.unionsubclass.secondarytables;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = {Payment.class, CardPayment.class})
@SessionFactory
public class UnionSubclassWithSecondaryTableTests {
	@Test
	@FailureExpected
	@Jira("https://hibernate.atlassian.net/browse/HHH-12676")
	void testIt(SessionFactoryScope sessions) {
		sessions.inTransaction( (session) -> {
			session.createQuery( "from CardPayment" ).getResultList();
			session.createQuery( "from Payment" ).getResultList();
		} );
	}
}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria.basic;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.domain.helpdesk.Ticket;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey(value = "HHH-12989")
@RequiresDialect(value = H2Dialect.class, comment = "Not dialect specific")
@DomainModel(standardModels = StandardDomainModel.HELPDESK)
@SessionFactory
public class InWithHeterogeneousCollectionTest {

	@Test
	public void testHeterogeneousInExpressions(SessionFactoryScope sessions) {
		sessions.inTransaction( (session) -> {
			final CriteriaBuilder cb = session.getCriteriaBuilder();

			final CriteriaQuery<Ticket> criteria = cb.createQuery( Ticket.class );
			final Root<Ticket> root = criteria.from( Ticket.class );
			final Path<String> keyPath = root.get( "key" );
			final Expression<String> lowercaseKey = cb.lower( keyPath );
			criteria.where( keyPath.in( Arrays.asList( lowercaseKey, "HHH-1" ) ) );
			session.createQuery( criteria ).getResultList();
		} );
	}
}

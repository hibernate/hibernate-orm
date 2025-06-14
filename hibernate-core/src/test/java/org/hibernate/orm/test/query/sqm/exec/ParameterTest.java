/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.sqm.exec;

import org.hibernate.query.Query;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.internal.SqmQueryImpl;

import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Steve Ebersole
 */
@DomainModel( standardModels = StandardDomainModel.RETAIL )
@SessionFactory
public class ParameterTest {
	@Test
	public void testReusedNamedParam(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "from SalesAssociate p where p.name.familiarName = :name or p.name.familyName = :name" )
							.setParameter( "name", "a name" )
							.list();
				}
		);
	}

	@Test
	public void testReusedOrdinalParam(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "from SalesAssociate p where p.name.familiarName = ?1 or p.name.familyName = ?1" )
							.setParameter( 1, "a name" )
							.list();
				}
		);
	}

	@Test
	public void testParametersWithQueryInterpretationCache(SessionFactoryScope scope) {
		String query = "from SalesAssociate p where p.name.familiarName in :names";
		scope.inTransaction(
				session -> {
					Query q = session.createQuery( query );
					DomainParameterXref xref = q.unwrap( SqmQueryImpl.class ).getDomainParameterXref();
					for ( QueryParameterImplementor<?> p : xref.getQueryParameters().keySet() ) {
						Assertions.assertTrue( q.getParameterMetadata().containsReference( p ) );
					}
				}
		);
		scope.inTransaction(
				session -> {
					Query q = session.createQuery( query );
					DomainParameterXref xref = q.unwrap( SqmQueryImpl.class ).getDomainParameterXref();
					for ( QueryParameterImplementor<?> p : xref.getQueryParameters().keySet() ) {
						Assertions.assertTrue( q.getParameterMetadata().containsReference( p ) );
					}
				}
		);
	}
}

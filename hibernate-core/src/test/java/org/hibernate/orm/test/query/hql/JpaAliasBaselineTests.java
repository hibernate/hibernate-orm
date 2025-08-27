/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql;

import org.hibernate.query.SemanticException;

import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Steve Ebersole
 */
@DomainModel( standardModels = StandardDomainModel.RETAIL )
@SessionFactory
public class JpaAliasBaselineTests {
	@Test
	public void testRootEntityAlias(SessionFactoryScope scope) {
		try {
			scope.inTransaction( (session) -> {
				session.createQuery( "select o from Order O where o.id = 1" ).list();
			} );

			fail( "Expecting exception" );
		}
		catch (IllegalArgumentException iae) {
			assertThat( iae.getCause() ).isInstanceOf( SemanticException.class );
		}
	}

	@Test
	public void testEntityJoinAlias(SessionFactoryScope scope) {
		try {
			scope.inTransaction( (session) -> {
				session.createQuery( "select o from LineItem i, Order O where i.order.id = o.id" ).list();
			} );

			fail( "Expecting exception" );
		}
		catch (IllegalArgumentException iae) {
			assertThat( iae.getCause() ).isInstanceOf( SemanticException.class );
		}
	}

	@Test
	public void testJoinAlias(SessionFactoryScope scope) {
		try {
			scope.inTransaction( (session) -> {
				session.createQuery( "select a from Order O join o.salesAssociate A" ).list();
			} );

			fail( "Expecting exception" );
		}
		catch (IllegalArgumentException iae) {
			assertThat( iae.getCause() ).isInstanceOf( SemanticException.class );
		}
	}

	@Test
	public void testSelectionAlias(SessionFactoryScope scope) {
		try {
			scope.inTransaction( (session) -> {
				session.createQuery( "select i.quantity as QTY from LineItem I where i.product.id = 1 order by qty" ).list();
			} );

			fail( "Expecting exception" );
		}
		catch (IllegalArgumentException iae) {
			assertThat( iae.getCause() ).isInstanceOf( SemanticException.class );
		}
	}
}

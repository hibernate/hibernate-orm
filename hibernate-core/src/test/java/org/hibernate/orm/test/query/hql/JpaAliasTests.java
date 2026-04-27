/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

/**
 * @author Steve Ebersole
 */
@ServiceRegistry(
		settings = @Setting( name = AvailableSettings.JPA_QUERY_COMPLIANCE, value = "true" )
)
@DomainModel( standardModels = StandardDomainModel.RETAIL )
@SessionFactory
public class JpaAliasTests {
	@Test
	public void testRootEntityAlias(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.createQuery( "select o from Order O where o.id = 1", Object.class ).list();
			session.createQuery( "select O from Order o where O.id = 1", Object.class ).list();
		} );
	}

	@Test
	public void testEntityJoinAlias(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.createQuery( "select o from LineItem i, Order O where i.order.id = o.id", Object.class ).list();
			session.createQuery( "select O from LineItem i, Order o where i.order.id = O.id", Object.class ).list();
		} );
	}

	@Test
	public void testJoinAlias(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.createQuery( "select a from Order O join o.salesAssociate A", Object.class ).list();
			session.createQuery( "select A from Order O join o.salesAssociate a", Object.class ).list();
		} );
	}

	@Test
	public void testSelectionAlias(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.createQuery( "select i.quantity as QTY from LineItem I where i.product.id = 1 order by qty", Object.class ).list();
			session.createQuery( "select I.quantity as qty from LineItem i where I.product.id = 1 order by QTY", Object.class ).list();
		} );
	}
}

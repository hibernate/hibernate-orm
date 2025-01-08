/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.usertypeaggregates;

import org.assertj.core.api.Assertions;
import org.hibernate.cfg.QuerySettings;
import org.hibernate.orm.test.usertypeaggregates.model.ClearingQuotaWithNamedQuery;
import org.hibernate.orm.test.usertypeaggregates.model.Decimal;
import org.hibernate.orm.test.usertypeaggregates.model.PojoRes;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

@DomainModel(annotatedClasses = ClearingQuotaWithNamedQuery.class)
@SessionFactory
@ServiceRegistry(settings = @Setting(name = QuerySettings.QUERY_STARTUP_CHECKING, value = "false"))
class NamedQueryTestCase {

	@BeforeAll
	static void init(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			ClearingQuotaWithNamedQuery cq1 = new ClearingQuotaWithNamedQuery();
			cq1.setQuotaCt( new Decimal( "1.15" ) );
			session.persist( cq1 );
			ClearingQuotaWithNamedQuery cq2 = new ClearingQuotaWithNamedQuery();
			cq2.setQuotaCt( new Decimal( "3.15" ) );
			session.persist( cq2 );
		} );
	}

	@AfterAll
	static void clear(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery( "delete from ClearingQuotaWithNamedQuery" ).executeUpdate() );
	}

	@Test
	public void sum(SessionFactoryScope scope) {
		PojoRes pojo = scope.fromSession(
				openSession -> openSession.createNamedQuery( ClearingQuotaWithNamedQuery.SELECT_SUM, PojoRes.class )
						.getSingleResult() );

		// then
		Assertions.assertThat( pojo.getRes() ).isInstanceOf( Decimal.class ).isEqualTo( new Decimal( "4.30" ) );
	}

	@Test
	public void average(SessionFactoryScope scope) {
		PojoRes pojo = scope.fromSession(
				openSession -> openSession.createNamedQuery( ClearingQuotaWithNamedQuery.SELECT_AVERAGE, PojoRes.class )
						.getSingleResult() );

		// then
		Assertions.assertThat( pojo.getRes() ).isInstanceOf( Decimal.class ).isEqualTo( new Decimal( "2.15" ) );
	}
}

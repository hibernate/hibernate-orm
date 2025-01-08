/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.usertypeaggregates;

import org.assertj.core.api.Assertions;
import org.hibernate.orm.test.usertypeaggregates.model.ClearingQuota;
import org.hibernate.orm.test.usertypeaggregates.model.Decimal;
import org.hibernate.orm.test.usertypeaggregates.model.PojoRes;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.MathContext;

@DomainModel(annotatedClasses = ClearingQuota.class)
@SessionFactory
class DynamicQueryTestCase {

	public static final Decimal SQRT_TWO = new Decimal( BigDecimal.valueOf( 2 ).sqrt( new MathContext( 17 ) ) );

	@BeforeEach
	void init(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			ClearingQuota cq1 = new ClearingQuota();
			cq1.setQuotaCt( new Decimal( "1.15" ) );
			session.persist( cq1 );
			ClearingQuota cq2 = new ClearingQuota();
			cq2.setQuotaCt( new Decimal( "3.15" ) );
			session.persist( cq2 );
		} );
	}

	@AfterEach
	void clear(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery( "delete from ClearingQuota" ).executeUpdate() );
	}

	@Test
	public void sum(SessionFactoryScope scope) {
		// when
		PojoRes pojo = scope.fromSession(
				openSession ->
						openSession.createQuery(
								"SELECT new org.hibernate.orm.test.usertypeaggregates.model.PojoRes(sum(c.quotaCt)) FROM ClearingQuota c",
								PojoRes.class ).getSingleResult() );

		// then
		Assertions.assertThat( pojo.getRes() ).isInstanceOf( Decimal.class ).isEqualTo( new Decimal( "4.30" ) );
	}

	@Test
	public void average(SessionFactoryScope scope) {
		// when
		PojoRes pojo = scope.fromSession(
				openSession ->
						openSession.createQuery(
								"SELECT new org.hibernate.orm.test.usertypeaggregates.model.PojoRes(avg(c.quotaCt)) FROM ClearingQuota c",
								PojoRes.class ).getSingleResult() );

		// then
		Assertions.assertThat( pojo.getRes() ).isInstanceOf( Decimal.class ).isEqualTo( new Decimal( "2.15" ) );
	}

	@Test
	public void var_pop(SessionFactoryScope scope) {
		// when
		PojoRes pojo = scope.fromSession(
				openSession ->
						openSession.createQuery(
								"SELECT new org.hibernate.orm.test.usertypeaggregates.model.PojoRes(var_pop(c.quotaCt)) FROM ClearingQuota c",
								PojoRes.class ).getSingleResult() );

		// then
		Assertions.assertThat( pojo.getRes() ).isInstanceOf( Decimal.class ).isEqualTo( new Decimal( "1" ) );
	}

	@Test
	public void var_samp(SessionFactoryScope scope) {
		// when
		PojoRes pojo = scope.fromSession(
				openSession ->
						openSession.createQuery(
								"SELECT new org.hibernate.orm.test.usertypeaggregates.model.PojoRes(var_samp(c.quotaCt)) FROM ClearingQuota c",
								PojoRes.class ).getSingleResult() );

		// then
		Assertions.assertThat( pojo.getRes() ).isInstanceOf( Decimal.class ).isEqualTo( new Decimal( "2" ) );
	}

	@Test
	public void stddev_pop(SessionFactoryScope scope) {
		// when
		PojoRes pojo = scope.fromSession(
				openSession ->
						openSession.createQuery(
								"SELECT new org.hibernate.orm.test.usertypeaggregates.model.PojoRes(stddev_pop(c.quotaCt)) FROM ClearingQuota c",
								PojoRes.class ).getSingleResult() );

		// then
		Assertions.assertThat( pojo.getRes() ).isInstanceOf( Decimal.class ).isEqualTo( new Decimal( "1" ) );
	}

	@Test
	public void stddev_samp(SessionFactoryScope scope) {
		// when
		PojoRes pojo = scope.fromSession(
				openSession ->
						openSession.createQuery(
								"SELECT new org.hibernate.orm.test.usertypeaggregates.model.PojoRes(stddev_samp(c.quotaCt)) FROM ClearingQuota c",
								PojoRes.class ).getSingleResult() );

		// then
		Assertions.assertThat( pojo.getRes() ).isInstanceOf( Decimal.class );

		Assertions.assertThat( pojo.getRes().subtract( SQRT_TWO ).abs() ).isLessThanOrEqualTo( new Decimal( "1e-16" ) );
	}
}

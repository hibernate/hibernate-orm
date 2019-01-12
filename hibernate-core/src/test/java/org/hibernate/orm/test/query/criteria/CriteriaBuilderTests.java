/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.criteria;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;

import org.hibernate.boot.MetadataSources;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.spi.CriteriaNodeBuilder;
import org.hibernate.query.criteria.spi.ExpressionImplementor;

import org.hibernate.testing.junit5.SessionFactoryBasedFunctionalTest;
import org.hibernate.testing.orm.domain.retail.RetailDomainModel;
import org.junit.jupiter.api.Test;

/**
 * Tests for Hibernate's {@link javax.persistence.criteria.CriteriaBuilder}
 * implementation - {@link org.hibernate.query.criteria.spi.CriteriaNodeBuilder}.
 *
 * Unit tests for the nodes created by the builder
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("WeakerAccess")
public class CriteriaBuilderTests extends SessionFactoryBasedFunctionalTest {
	@Override
	protected void applyMetadataSources(MetadataSources metadataSources) {
		super.applyMetadataSources( metadataSources );
		RetailDomainModel.applyRetailModel( metadataSources );
	}

	@Test
	public void aggregationFunctionSmokeTest() {
		final HibernateCriteriaBuilder criteriaBuilder = getCriteriaBuilder();

		final JpaExpression<Integer> one = criteriaBuilder.literal( 1 );
		final JpaExpression<Float> oneF = criteriaBuilder.literal( 1f );

		checkNumericExpression( criteriaBuilder.abs( one ) );
		checkNumericExpression( criteriaBuilder.abs( oneF ) );
		checkNumericExpression( criteriaBuilder.avg( one ) );
		checkNumericExpression( criteriaBuilder.avg( oneF ) );

		checkNumericExpression( criteriaBuilder.count( one ) );
		checkNumericExpression( criteriaBuilder.count( oneF ) );

		checkNumericExpression( criteriaBuilder.greatest( one ) );
		checkNumericExpression( criteriaBuilder.greatest( oneF ) );
		checkNumericExpression( criteriaBuilder.least( one ) );
		checkNumericExpression( criteriaBuilder.least( oneF ) );

		checkNumericExpression( criteriaBuilder.max( one ) );
		checkNumericExpression( criteriaBuilder.max( oneF ) );
		checkNumericExpression( criteriaBuilder.min( one ) );
		checkNumericExpression( criteriaBuilder.min( oneF ) );

		checkNumericExpression( criteriaBuilder.sum( one ) );
		checkNumericExpression( criteriaBuilder.sum( oneF ) );
		checkNumericExpression( criteriaBuilder.sumAsLong( one ) );
		checkNumericExpression( criteriaBuilder.sumAsDouble( oneF ) );
	}

	private void checkNumericExpression(JpaExpression<? extends Number> expr) {
		checkExpression( expr );

	}

	private void checkExpression(JpaExpression<?> expr) {
		assert expr.as( String.class ) != expr;
		assert expr.asString() == expr;
	}

	private CriteriaNodeBuilder getCriteriaBuilder() {
		return sessionFactory().getCriteriaBuilder();
	}

	@Test
	public void temporalFunctionSmokeTest() {
		checkTemporalExpression( getCriteriaBuilder().currentDate() );
		checkTemporalExpression( getCriteriaBuilder().currentTime() );
		checkTemporalExpression( getCriteriaBuilder().currentTimestamp() );
	}

	private void checkTemporalExpression(ExpressionImplementor temporalExpression) {
		checkExpression( temporalExpression );
	}
}

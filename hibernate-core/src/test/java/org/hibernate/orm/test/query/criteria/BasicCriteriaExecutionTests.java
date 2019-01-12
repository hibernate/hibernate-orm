/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.criteria;

import org.hibernate.boot.MetadataSources;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaParameterExpression;
import org.hibernate.query.criteria.JpaRoot;
import org.hibernate.query.criteria.spi.CriteriaNodeBuilder;
import org.hibernate.query.criteria.spi.ExpressionImplementor;

import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.domain.gambit.BasicEntity;
import org.hibernate.testing.orm.junit.BaseSessionFactoryFunctionalTest;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.junit.jupiter.api.Test;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("WeakerAccess")
public class BasicCriteriaExecutionTests extends BaseSessionFactoryFunctionalTest  {
	@Override
	protected void applyMetadataSources(MetadataSources metadataSources) {
		super.applyMetadataSources( metadataSources );
		StandardDomainModel.GAMBIT.getDescriptor().applyDomainModel( metadataSources );
	}

	@Test
	public void testExecutingBasicCriteriaQuery() {
		final CriteriaNodeBuilder criteriaBuilder = sessionFactory().getQueryEngine().getCriteriaBuilder();

		final JpaCriteriaQuery<Object> criteria = criteriaBuilder.createQuery();

		final JpaRoot<BasicEntity> root = criteria.from( BasicEntity.class );

		criteria.select( root );

		sessionFactoryScope().inSession(
				session -> session.createQuery( criteria ).list()
		);
	}

	@Test
//	@FailureExpected( "Criteria nodes do not currently track ExpressableType + StackOverflow resolving implied type" )
	public void testExecutingBasicCriteriaQueryLiteralPredicate() {
		final CriteriaNodeBuilder criteriaBuilder = sessionFactory().getQueryEngine().getCriteriaBuilder();

		final JpaCriteriaQuery<Object> criteria = criteriaBuilder.createQuery();

		final JpaRoot<BasicEntity> root = criteria.from( BasicEntity.class );

		criteria.select( root );

		final ExpressionImplementor<Integer> one = criteriaBuilder.literal( 1 );

		criteria.where( criteriaBuilder.equal( one, one ) );

		sessionFactoryScope().inSession(
				session -> session.createQuery( criteria ).list()
		);
	}

	@Test
//	@FailureExpected( "Criteria nodes do not currently track ExpressableType" )
	public void testExecutingBasicCriteriaQueryParameterPredicate() {
		final CriteriaNodeBuilder criteriaBuilder = sessionFactory().getQueryEngine().getCriteriaBuilder();

		final JpaCriteriaQuery<Object> criteria = criteriaBuilder.createQuery();

		final JpaRoot<BasicEntity> root = criteria.from( BasicEntity.class );

		criteria.select( root );

		final JpaParameterExpression<Integer> param = criteriaBuilder.parameter( Integer.class );

		criteria.where( criteriaBuilder.equal( param, param ) );

		sessionFactoryScope().inSession(
				session -> session.createQuery( criteria )
						.setParameter( param, 1 )
						.list()
		);
	}
}

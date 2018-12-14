/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.criteria;

import java.util.Set;
import javax.persistence.criteria.ParameterExpression;

import org.hibernate.boot.MetadataSources;
import org.hibernate.orm.test.support.domains.AvailableDomainModel;
import org.hibernate.orm.test.support.domains.gambit.BasicEntity;
import org.hibernate.orm.test.support.domains.gambit.BasicEntity_;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaRoot;
import org.hibernate.query.criteria.spi.CriteriaNodeBuilder;

import org.hibernate.testing.junit5.FailureExpected;
import org.hibernate.testing.junit5.SessionFactoryBasedFunctionalTest;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("WeakerAccess")
public class BasicCriteriaBuildingTests extends SessionFactoryBasedFunctionalTest {
	@Override
	protected void applyMetadataSources(MetadataSources metadataSources) {
		super.applyMetadataSources( metadataSources );
		AvailableDomainModel.GAMBIT.getDomainModel().applyDomainModel( metadataSources );
	}

	@Test
	public void testParameterCollecting() {
		final CriteriaNodeBuilder criteriaBuilder = sessionFactory().getQueryEngine().getCriteriaBuilder();
		final JpaCriteriaQuery<Object> criteria = criteriaBuilder.createQuery();

		final JpaRoot<BasicEntity> root = criteria.from( BasicEntity.class );

		criteria.select( root );

		criteria.where(
				criteriaBuilder.equal(
						// grr, see below
						//root.get( BasicEntity_.data ),
						root.get( BasicEntity_.DATA ),
						criteriaBuilder.parameter( String.class )
				)
		);

		final Set<ParameterExpression<?>> parameters = criteria.getParameters();
		assertThat( parameters, notNullValue() );
		assertThat( parameters.size(), is( 1 ) );
	}

	@Test
	@FailureExpected( "It seems we do not populate the 'JPA static metamodel'" )
	public void testStaticMetamodelUsage() {
		assertThat( BasicEntity_.data, notNullValue() );
	}
}

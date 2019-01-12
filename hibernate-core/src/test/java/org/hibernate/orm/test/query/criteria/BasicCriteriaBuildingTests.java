/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.criteria;

import java.util.Set;
import javax.persistence.criteria.ParameterExpression;

import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaRoot;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistry.Setting;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.domain.gambit.BasicEntity;
import org.hibernate.testing.orm.domain.gambit.BasicEntity_;
import org.junit.jupiter.api.Test;

import org.hamcrest.CoreMatchers;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Steve Ebersole
 */
@DomainModel(
		standardModels = StandardDomainModel.GAMBIT
)
@ServiceRegistry(
		settings = @Setting(
				name = AvailableSettings.HBM2DDL_AUTO,
				value = "create-drop"
		)
)
@SessionFactory
@SuppressWarnings("WeakerAccess")
public class BasicCriteriaBuildingTests {
	@Test
	public void testParameterCollecting(SessionFactoryScope scope) {
		assertThat( scope, notNullValue() );
		final HibernateCriteriaBuilder criteriaBuilder = scope.getSessionFactory().getQueryEngine().getCriteriaBuilder();
		final JpaCriteriaQuery<Object> criteria = criteriaBuilder.createQuery();

		final JpaRoot<BasicEntity> root = criteria.from( BasicEntity.class );

		criteria.select( root );

		criteria.where(
				criteriaBuilder.equal(
						// grr, see below
						root.get( BasicEntity_.data ),
						//root.get( BasicEntity_.DATA ),
						criteriaBuilder.parameter( String.class )
				)
		);

		final Set<ParameterExpression<?>> parameters = criteria.getParameters();
		assertThat( parameters, notNullValue() );
		assertThat( parameters.size(), is( 1 ) );
	}

	@Test
	public void testMultipleInjections(SessionFactoryScope scope, MetadataImplementor model) {
		assertThat( scope, CoreMatchers.notNullValue() );
		assertThat( model, CoreMatchers.notNullValue() );
	}
}

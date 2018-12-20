/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.criteria;

import java.util.Set;
import javax.persistence.criteria.ParameterExpression;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaRoot;

import org.hibernate.testing.junit5.FailureExpected;
import org.hibernate.testing.orm.ServiceRegistry;
import org.hibernate.testing.orm.ServiceRegistry.Setting;
import org.hibernate.testing.orm.SessionFactory;
import org.hibernate.testing.orm.SessionFactoryScope;
import org.hibernate.testing.orm.SessionFactoryScopeInjectable;
import org.hibernate.testing.orm.TestDomain;
import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.domain.gambit.BasicEntity;
import org.hibernate.testing.orm.domain.gambit.BasicEntity_;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Steve Ebersole
 */
@TestDomain(
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
public class BasicCriteriaBuildingTests implements SessionFactoryScopeInjectable {
	private SessionFactoryScope scope;

	@Test
	public void testParameterCollecting() {
		final HibernateCriteriaBuilder criteriaBuilder = scope.getSessionFactory().getQueryEngine().getCriteriaBuilder();
		final JpaCriteriaQuery<Object> criteria = criteriaBuilder.createQuery();

		final JpaRoot<BasicEntity> root = criteria.from( BasicEntity.class );

		criteria.select( root );

		criteria.where(
				criteriaBuilder.equal(
						// grr, see below
						//root.get( BasicEntity_.data ),
						root.get( BasicEntity_.data ),
						criteriaBuilder.parameter( String.class )
				)
		);

		final Set<ParameterExpression<?>> parameters = criteria.getParameters();
		assertThat( parameters, notNullValue() );
		assertThat( parameters.size(), is( 1 ) );
	}

	@Override
	public void injectSessionFactoryScope(SessionFactoryScope scope) {
		this.scope = scope;
	}
}

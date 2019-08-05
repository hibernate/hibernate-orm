/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.hql;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.hibernate.orm.test.query.sqm.BaseSqmUnitTest.interpretSelect;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("WeakerAccess")
@DomainModel(
		standardModels = StandardDomainModel.GAMBIT
)
@ServiceRegistry(
		settings = @ServiceRegistry.Setting(
				name = AvailableSettings.HBM2DDL_AUTO,
				value = "create-drop"
		)
)
@SessionFactory
public class PagingTests {

	@Test
	public void testPagingByParameter(SessionFactoryScope scope) {
		interpretSelect( "select o from SimpleEntity o offset :param", scope.getSessionFactory() );
		interpretSelect( "select o from SimpleEntity o limit :param", scope.getSessionFactory() );
		interpretSelect( "select o from SimpleEntity o limit :param offset :param", scope.getSessionFactory() );
	}

	@Test
	public void testPagingByConstant(SessionFactoryScope scope) {
		interpretSelect( "select o from SimpleEntity o offset 1", scope.getSessionFactory() );
		interpretSelect( "select o from SimpleEntity o limit 1", scope.getSessionFactory() );
		interpretSelect( "select o from SimpleEntity o limit 1 offset 1", scope.getSessionFactory() );
	}

	@Test
	public void testPagingOnSubQuery(SessionFactoryScope scope) {
		interpretSelect( "select o from SimpleEntity o where o.someString = ( select oSub.someString from SimpleEntity oSub order by oSub.someString limit 1 )", scope.getSessionFactory() );
	}
}

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.query.hql;


import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Steve Ebersole
 */
@DomainModel( standardModels = StandardDomainModel.RETAIL )
@SessionFactory( useCollectingStatementInspector = true )
public class JpaCrossJoinBaselineTests {
	@Test
	public void testCrossJoin(SessionFactoryScope scope) {
		final String qry = "from LineItem i cross join Order o join o.salesAssociate a on i.product.vendor.name = a.name.familyName";
		scope.inTransaction( (session) -> session.createQuery( qry ).list() );
	}

	@Test
	public void test2Roots(SessionFactoryScope scope) {
		final String qry = "select i from LineItem i, Order o join o.salesAssociate a on i.quantity = a.id";

		try {
			scope.inTransaction( (session) -> session.createQuery( qry ).list() );
			fail( "Expecting a failure" );
		}
		catch (Exception expected) {
		}
	}

	@Test
	public void test2Roots2(SessionFactoryScope scope) {
		final String qry = "from LineItem i, Order o join o.salesAssociate a on i.product.vendor.name = a.name.familyName";

		try {
			scope.inTransaction( (session) -> session.createQuery( qry ).list() );
			fail( "Expecting a failure" );
		}
		catch (Exception expected) {
		}
	}
}

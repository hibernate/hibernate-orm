package org.hibernate.orm.test.query.hql;


import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Steve Ebersole
 */
// Retain Product's legacy generated version while testing the retail model.
@ServiceRegistry(settings = @Setting(name = MappingSettings.TEMPORAL_PRESERVATION, value = "false"))
@DomainModel( standardModels = StandardDomainModel.RETAIL )
@SessionFactory( useCollectingStatementObserver = true )
public class JpaCrossJoinBaselineTests {
	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsNestedJoinGroups.class)
	public void testCrossJoin(SessionFactoryScope scope) {
		final String qry = "from LineItem i cross join Order o join o.salesAssociate a on i.product.vendor.name = a.name.familyName";
		scope.inTransaction( (session) -> session.createQuery( qry, Object[].class ).list() );
	}

	@Test
	public void test2Roots(SessionFactoryScope scope) {
		final String qry = "select i from LineItem i, Order o join o.salesAssociate a on i.quantity = a.id";

		try {
			scope.inTransaction( (session) -> session.createQuery( qry, Object[].class ).list() );
			fail( "Expecting a failure" );
		}
		catch (Exception expected) {
		}
	}

	@Test
	public void test2Roots2(SessionFactoryScope scope) {
		final String qry = "from LineItem i, Order o join o.salesAssociate a on i.product.vendor.name = a.name.familyName";

		try {
			scope.inTransaction( (session) -> session.createQuery( qry, Object[].class ).list() );
			fail( "Expecting a failure" );
		}
		catch (Exception expected) {
		}
	}
}

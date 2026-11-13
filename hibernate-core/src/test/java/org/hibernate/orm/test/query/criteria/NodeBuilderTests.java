package org.hibernate.orm.test.query.criteria;

import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.query.sqm.spi.NodeBuilder;
import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.domain.retail.Order;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

/**
 * Simple {@link NodeBuilder} tests
 *
 * @author Steve Ebersole
 */
// Retain Product's legacy generated version while testing the retail model.
@ServiceRegistry(settings = @Setting(name = MappingSettings.TEMPORAL_PRESERVATION, value = "false"))
@DomainModel( standardModels = StandardDomainModel.RETAIL )
@SessionFactory
public class NodeBuilderTests {
	@Test
	public void testFkExpression(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final String hql = "from Order o where fk(o.salesAssociate) = 1";
			session.createSelectionQuery( hql, Order.class ).getResultList();
		} );
	}
}

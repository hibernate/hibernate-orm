/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.criteria;

import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

/**
 * Simple {@link org.hibernate.query.sqm.NodeBuilder} tests
 *
 * @author Steve Ebersole
 */
@DomainModel( standardModels = StandardDomainModel.RETAIL )
@SessionFactory
public class NodeBuilderTests {
	@Test
	public void testFkExpression(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final String hql = "from Order o where fk(o.salesAssociate) = 1";
			session.createSelectionQuery( hql ).getResultList();
		} );
	}
}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.pagination.hhh9965;

import org.hibernate.HibernateException;
import org.hibernate.cfg.Environment;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Created on 17/12/17.
 *
 * @author Reda.Housni-Alaoui
 */
@JiraKey(value = "HHH-9965")
@DomainModel(
		annotatedClasses = {
				Shop.class,
				Product.class
		}
)
@ServiceRegistry(
		settings = @Setting(name = Environment.FAIL_ON_PAGINATION_OVER_COLLECTION_FETCH, value = "true")
)
@SessionFactory
public class HHH9965Test {

	@Test
	public void testHHH9965(SessionFactoryScope scope) {
		assertThrows( HibernateException.class, () -> scope.inTransaction(
				session -> {
					session.createSelectionQuery( "SELECT s FROM Shop s join fetch s.products", Shop.class )
							.setMaxResults( 3 )
							.list();
					fail( "Pagination over collection fetch failure was expected" );
				} )
		);
	}
}

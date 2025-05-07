/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect.function;

import org.hibernate.dialect.MySQLDialect;
import org.hibernate.query.Query;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

/**
 * @author Strong Liu
 */
@RequiresDialect(MySQLDialect.class)
@DomainModel(annotatedClasses = Product.class)
@SessionFactory
@SuppressWarnings("JUnitMalformedDeclaration")
class MySQLRoundFunctionTest {
	@AfterEach
	public void tearDown(SessionFactoryScope factoryScope) throws Exception {
		factoryScope.dropData();
	}

	@Test
	void testRoundFunction(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (s) -> {
			Product product = new Product(1L);
			product.setLength( 100 );
			product.setPrice( new BigDecimal( 1.298 ) );
			s.persist( product );
		} );

		factoryScope.inTransaction( (s) -> {
			Query q=s.createQuery( "select round(p.price,1) from Product p" );
			Object o=q.uniqueResult();
			Assertions.assertEquals( BigDecimal.class, o.getClass() );
			Assertions.assertEquals( BigDecimal.valueOf( 1.3 ), o );
		} );
	}

}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect.function;
import java.math.BigDecimal;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import org.hibernate.dialect.MySQLDialect;
import org.hibernate.testing.orm.junit.RequiresDialect;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
@author Strong Liu
 */
@RequiresDialect( MySQLDialect.class )
@DomainModel(xmlMappings = {"org/hibernate/orm/test/dialect/function/Product.hbm.xml"})
@SessionFactory
public class MySQLRoundFunctionTest {

	@Test
	public void testRoundFunction(SessionFactoryScope scope){
		scope.inTransaction(  session -> {
			Product product = new Product();
			product.setLength( 100 );
			product.setPrice( new BigDecimal( 1.298 ) );
			session.persist( product );
		} );
		scope.inTransaction(  session -> {
			BigDecimal bd = session.createQuery( "select round(p.price,1) from Product p", BigDecimal.class ).uniqueResult();
			assertEquals( BigDecimal.valueOf( 1.3 ), bd );
		} );
	}

}

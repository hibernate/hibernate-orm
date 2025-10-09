/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;

import org.hibernate.dialect.HANADialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@RequiresDialect(HANADialect.class)
@DomainModel(xmlMappings = {"org/hibernate/orm/test/dialect/function/Product.hbm.xml"})
@SessionFactory
public class HANAFunctionsTest {

	@BeforeEach
	public void setup(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Product product = new Product();
			product.setLength( 100 );
			product.setPrice( new BigDecimal( "1.298" ) );
			session.persist( product );
		} );
	}

	@Test
	@JiraKey(value = "HHH-12546")
	public void testLocateFunction(SessionFactoryScope scope) {
		scope.inTransaction(  session -> {
			Product p = session.createQuery( "select p from Product p where locate('.', cast(p.price as string)) > 0", Product.class ).uniqueResult();
			assertNotNull( p );
			assertEquals( 100, p.getLength() );
			assertEquals( BigDecimal.valueOf( 1.29 ), p.getPrice() );
		} );
		scope.inTransaction(  session -> {
			Product p = session.createQuery( "select p from Product p where locate('.', cast(p.price as string)) = 0", Product.class ).uniqueResult();
			assertNull( p );
		} );

		scope.inTransaction(  session -> {
			Product p = session.createQuery( "select p from Product p where locate('.', cast(p.price as string), 3) > 0", Product.class ).uniqueResult();
			assertNull( p );
		} );

	}

	@Test
	public void testSubstringFunction(SessionFactoryScope scope) {
		scope.inTransaction(  session -> {
			Product p = session.createQuery( "select p from Product p where substring(cast(p.price as string), 1, 2) = '1.'", Product.class ).uniqueResult();
			assertNotNull( p );
			assertEquals( 100, p.getLength() );
			assertEquals( BigDecimal.valueOf( 1.29 ), p.getPrice() );
		} );

		scope.inTransaction(  session -> {
			Product p = session.createQuery( "select p from Product p where substring(cast(p.price as string), 1, 2) = '.1'", Product.class ).uniqueResult();
			assertNull( p );
		} );

		scope.inTransaction(  session -> {
			Product p = session.createQuery( "select p from Product p where substring(cast(p.price as string), 1) = '1.29'", Product.class ).uniqueResult();
			assertNotNull( p );
			assertEquals( 100, p.getLength() );
			assertEquals( BigDecimal.valueOf( 1.29 ), p.getPrice() );
		} );

		scope.inTransaction(   session -> {
			Product p = session.createQuery( "select p from Product p where substring(cast(p.price as string), 1) = '1.'", Product.class ).uniqueResult();
			assertNull( p );
		} );
	}

}

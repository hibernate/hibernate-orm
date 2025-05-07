/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect.function;

import org.hibernate.Transaction;
import org.hibernate.dialect.HANADialect;
import org.hibernate.query.Query;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@RequiresDialect(HANADialect.class)
@DomainModel(annotatedClasses = Product.class)
@SessionFactory
@SuppressWarnings("JUnitMalformedDeclaration")
public class HANAFunctionsTest {
	@BeforeEach
	void setUp(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (s) -> {
			Product product = new Product();
			product.setLength( 100 );
			product.setPrice( new BigDecimal( 1.298 ) );
			s.persist( product );
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	@JiraKey(value = "HHH-12546")
	public void testLocateFunction(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (s) -> {
			Query<Product> q = s.createQuery( "select p from Product p where locate('.', cast(p.price as string)) > 0", Product.class );
			Product p = q.uniqueResult();
			assertNotNull( p );
			assertEquals( 100, p.getLength() );
			assertEquals( BigDecimal.valueOf( 1.29 ), p.getPrice() );
		} );

		factoryScope.inTransaction( (s) -> {
			Query<Product> q = s.createQuery( "select p from Product p where locate('.', cast(p.price as string)) = 0", Product.class );
			Product p = q.uniqueResult();
			assertNull( p );
		} );

		factoryScope.inTransaction( (s) -> {
			Transaction tx = s.beginTransaction();
			Query<Product> q = s.createQuery( "select p from Product p where locate('.', cast(p.price as string), 3) > 0", Product.class );
			Product p = q.uniqueResult();
			assertNull( p );
		} );
	}

	@Test
	public void testSubstringFunction(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (s) -> {
			Query<Product> q = s.createQuery( "select p from Product p where substring(cast(p.price as string), 1, 2) = '1.'", Product.class );
			Product p = q.uniqueResult();
			assertNotNull( p );
			assertEquals( 100, p.getLength() );
			assertEquals( BigDecimal.valueOf( 1.29 ), p.getPrice() );
		} );

		factoryScope.inTransaction( (s) -> {
			Query<Product> q = s.createQuery( "select p from Product p where substring(cast(p.price as string), 1, 2) = '.1'", Product.class );
			Product p = q.uniqueResult();
			assertNull( p );
		} );

		factoryScope.inTransaction( (s) -> {
			Query<Product> q = s.createQuery( "select p from Product p where substring(cast(p.price as string), 1) = '1.29'", Product.class );
			Product p = q.uniqueResult();
			assertNotNull( p );
			assertEquals( 100, p.getLength() );
			assertEquals( BigDecimal.valueOf( 1.29 ), p.getPrice() );
		} );

		factoryScope.inTransaction( (s) -> {
			Query<Product> q = s.createQuery( "select p from Product p where substring(cast(p.price as string), 1) = '1.'", Product.class );
			Product p = q.uniqueResult();
			assertNull( p );
		} );
	}

}

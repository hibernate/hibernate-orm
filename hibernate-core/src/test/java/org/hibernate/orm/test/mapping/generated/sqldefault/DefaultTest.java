/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.generated.sqldefault;

import java.math.BigDecimal;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Generated;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.junit.Assert.assertEquals;

/**
 * @author Gavin King
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = DefaultTest.OrderLine.class)
@SessionFactory
public class DefaultTest {

	@Test
	public void test(SessionFactoryScope scope) {
		BigDecimal unitPrice = new BigDecimal("12.99");
		scope.inTransaction( session -> {
			OrderLine entity = new OrderLine( unitPrice, 5 );
			session.persist(entity);
			session.flush();
			assertEquals( "new", entity.status );
			assertEquals( unitPrice, entity.unitPrice );
			assertEquals( 5, entity.quantity );
		} );
		scope.inTransaction( session -> {
			OrderLine entity = session.createQuery("from WithDefault", OrderLine.class ).getSingleResult();
			assertEquals( unitPrice, entity.unitPrice );
			assertEquals( 5, entity.quantity );
			assertEquals( "new", entity.status );
			entity.status = "old";
		} );
		scope.inTransaction( session -> {
			OrderLine entity = session.createQuery("from WithDefault", OrderLine.class ).getSingleResult();
			assertEquals( unitPrice, entity.unitPrice );
			assertEquals( 5, entity.quantity );
			assertEquals( "old", entity.status );
		} );
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Entity(name="WithDefault")
	public static class OrderLine {
		@Id
		private BigDecimal unitPrice;
		@Id @ColumnDefault(value = "1")
		private int quantity;
		@Generated
		@ColumnDefault(value = "'new'")
		private String status;

		public OrderLine() {}
		public OrderLine(BigDecimal unitPrice, int quantity) {
			this.unitPrice = unitPrice;
			this.quantity = quantity;
		}
	}
}

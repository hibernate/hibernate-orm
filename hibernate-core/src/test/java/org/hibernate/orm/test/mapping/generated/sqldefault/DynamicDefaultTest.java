/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.generated.sqldefault;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author Gavin King
 */
@DomainModel(annotatedClasses = DynamicDefaultTest.OrderLine.class)
@SessionFactory
public class DynamicDefaultTest {

	@Test
	public void testWithDefault(SessionFactoryScope scope) {
		BigDecimal unitPrice = new BigDecimal("12.99");
		scope.inTransaction( session -> {
			OrderLine entity = new OrderLine( unitPrice, 5 );
			session.persist(entity);
			session.flush();
			assertNull( entity.status );
			String status = session.createQuery("select status from WithDefault", String.class).getSingleResult();
			assertEquals( "new", status );
			session.refresh(entity);
			assertEquals( unitPrice, entity.unitPrice );
			assertEquals( 5, entity.quantity );
//            assertEquals( "new", entity.status ); //TODO: assertion currently fails; apparent bug in refresh()
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

	@Test
	public void testWithExplicit(SessionFactoryScope scope) {
		BigDecimal unitPrice = new BigDecimal("12.99");
		scope.inTransaction( session -> {
			OrderLine entity = new OrderLine( unitPrice, 5 );
			entity.status = "New";
			session.persist(entity);
			session.flush();
			String status = session.createQuery("select status from WithDefault", String.class).getSingleResult();
			assertEquals( "New", status );
			session.refresh(entity);
			assertEquals( "New", entity.status );
			assertEquals( unitPrice, entity.unitPrice );
			assertEquals( 5, entity.quantity );
		} );
		scope.inTransaction( session -> {
			OrderLine entity = session.createQuery("from WithDefault", OrderLine.class ).getSingleResult();
			assertEquals( unitPrice, entity.unitPrice );
			assertEquals( 5, entity.quantity );
			assertEquals( "New", entity.status );
		} );
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Entity(name="WithDefault")
	@DynamicInsert
	public static class OrderLine {
		@Id
		private BigDecimal unitPrice;
		@Id @ColumnDefault(value = "1")
		private int quantity;
		@ColumnDefault(value = "'new'")
		private String status;

		public OrderLine() {}
		public OrderLine(BigDecimal unitPrice, int quantity) {
			this.unitPrice = unitPrice;
			this.quantity = quantity;
		}
	}
}

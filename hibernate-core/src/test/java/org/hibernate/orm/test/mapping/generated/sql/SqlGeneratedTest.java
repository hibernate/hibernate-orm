/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.generated.sql;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.Generated;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.generator.EventType;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * @author Gavin King
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = SqlGeneratedTest.OrderLine.class)
@SessionFactory
@SkipForDialect(dialectClass = SybaseASEDialect.class,
		reason = "The name 'current_timestamp' is illegal in this context. Only constants, constant expressions, or variables allowed here.")
public class SqlGeneratedTest {

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
			assertNotNull( entity.updated );
		} );
		scope.inTransaction( session -> {
			OrderLine entity = session.createQuery("from WithDefault", OrderLine.class ).getSingleResult();
			assertEquals( unitPrice, entity.unitPrice );
			assertEquals( 5, entity.quantity );
			assertEquals( "new", entity.status );
			assertNotNull( entity.updated );
			entity.status = "old";
		} );
		scope.inTransaction( session -> {
			OrderLine entity = session.createQuery("from WithDefault", OrderLine.class ).getSingleResult();
			assertEquals( unitPrice, entity.unitPrice );
			assertEquals( 5, entity.quantity );
			assertEquals( "old", entity.status );
			assertNotNull( entity.updated );
			LocalDateTime previous = entity.updated;
			entity.quantity = 10;
			session.flush();
			assertNotNull( entity.updated );
			assertFalse( previous == entity.updated );
		} );
		scope.inTransaction( session -> {
			OrderLine entity = session.createQuery("from WithDefault", OrderLine.class ).getSingleResult();
			assertEquals( unitPrice, entity.unitPrice );
			assertEquals( 10, entity.quantity );
			assertEquals( "old", entity.status );
			assertNotNull( entity.updated );
		} );
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Entity(name="WithDefault")
	public static class OrderLine {
		@Id
		private long id;
		private BigDecimal unitPrice;
		private int quantity = 1;
		@Generated(sql = "'new'")
		private String status;
		@Generated(event = {EventType.INSERT, EventType.UPDATE},
				sql = "current_timestamp")
		private LocalDateTime updated;


		public OrderLine() {}
		public OrderLine(BigDecimal unitPrice, int quantity) {
			this.unitPrice = unitPrice;
			this.quantity = quantity;
		}
	}
}

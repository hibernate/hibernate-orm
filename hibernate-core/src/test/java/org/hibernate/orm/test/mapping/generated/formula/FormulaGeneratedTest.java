/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.generated.formula;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Gavin King
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = FormulaGeneratedTest.OrderLine.class)
@SessionFactory
//@ServiceRegistry(settings = @Setting(name = USE_GET_GENERATED_KEYS, value = "false"))
public class FormulaGeneratedTest {

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
			assertEquals( 64.95f, entity.total.floatValue(), 0.01f );
		} );
		scope.inTransaction( session -> {
			OrderLine entity = session.createQuery("from WithDefault", OrderLine.class ).getSingleResult();
			assertEquals( unitPrice, entity.unitPrice );
			assertEquals( 5, entity.quantity );
			assertEquals( "new", entity.status );
			assertEquals( 64.95f, entity.total.floatValue(), 0.01f );
			entity.status = "old"; //should be ignored when fetch=true
		} );
		scope.inTransaction( session -> {
			OrderLine entity = session.createQuery("from WithDefault", OrderLine.class ).getSingleResult();
			assertEquals( unitPrice, entity.unitPrice );
			assertEquals( 5, entity.quantity );
			assertEquals( "new", entity.status );
			assertEquals( 64.95f, entity.total.floatValue(), 0.01f );
			entity.quantity = 10;
			session.flush();
			assertEquals( 129.90f, entity.total.floatValue(), 0.01f );
		} );
		scope.inTransaction( session -> {
			OrderLine entity = session.createQuery("from WithDefault", OrderLine.class ).getSingleResult();
			assertEquals( unitPrice, entity.unitPrice );
			assertEquals( 10, entity.quantity );
			assertEquals( "new", entity.status );
			assertEquals( 129.90f, entity.total.floatValue(), 0.01f );
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
		@Generated
		@Formula(value = "'new'")
		private String status;
		@Generated(event = {EventType.INSERT, EventType.UPDATE})
		@Formula(value = "quantity*unitPrice")
		private BigDecimal total;


		public OrderLine() {}
		public OrderLine(BigDecimal unitPrice, int quantity) {
			this.unitPrice = unitPrice;
			this.quantity = quantity;
		}
	}
}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.generated.sqldefault;

import java.math.BigDecimal;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DialectOverride;
import org.hibernate.annotations.Generated;
import org.hibernate.dialect.H2Dialect;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Gavin King
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = OverriddenDefaultTest.OrderLine.class)
@SessionFactory
public class OverriddenDefaultTest {

	@Test
	public void test(SessionFactoryScope scope) {
		BigDecimal unitPrice = new BigDecimal("12.99");
		scope.inTransaction( session -> {
			OrderLine entity = new OrderLine( unitPrice, 5 );
			session.persist(entity);
			session.flush();
			assertEquals( getDefault(scope), entity.status );
			assertEquals( unitPrice, entity.unitPrice );
			assertEquals( 5, entity.quantity );
		} );
		scope.inTransaction( session -> {
			OrderLine entity = session.createQuery("from WithDefault", OrderLine.class ).getSingleResult();
			assertEquals( unitPrice, entity.unitPrice );
			assertEquals( 5, entity.quantity );
			assertEquals( getDefault(scope), entity.status );
			entity.status = "old"; //should be ignored when fetch=true
		} );
		scope.inTransaction( session -> {
			OrderLine entity = session.createQuery("from WithDefault", OrderLine.class ).getSingleResult();
			assertEquals( unitPrice, entity.unitPrice );
			assertEquals( 5, entity.quantity );
			assertEquals( "old", entity.status );
		} );
	}

	String getDefault(SessionFactoryScope scope) {
		return scope.getMetadataImplementor().getDatabase().getDialect() instanceof H2Dialect ? "NEW" : "new";
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Entity(name="WithDefault")
	public static class OrderLine {
		@Id
		private BigDecimal unitPrice;
		@Id @ColumnDefault("1")
		private int quantity;
		@Generated
		@ColumnDefault("'new'")
		@DialectOverride.ColumnDefault(dialect = H2Dialect.class,
				sameOrAfter = @DialectOverride.Version(major=1, minor=4),
				override = @ColumnDefault("'NEW'"))
		private String status;

		public OrderLine() {}
		public OrderLine(BigDecimal unitPrice, int quantity) {
			this.unitPrice = unitPrice;
			this.quantity = quantity;
		}
	}
}

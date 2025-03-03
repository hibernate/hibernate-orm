/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.binding.annotations.plural.orderby.compliance;

import java.util.Set;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.ordering.ast.OrderByComplianceViolation;

import org.hibernate.testing.transaction.TransactionUtil2;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Steve Ebersole
 */
public class OrderByMappingComplianceTest {

	@Test
	public void testComplianceChecking() {
		check( false );

		try {
			check( true );
			fail( "Expecting an exception" );
		}
		catch (OrderByComplianceViolation expected) {
			// nothing to do
		}
	}

	private void check(boolean complianceEnabled) {
		final StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( AvailableSettings.HBM2DDL_AUTO, "create-drop" )
				.build();

		try {
			final Metadata bootModel = new MetadataSources( ssr )
					.addAnnotatedClass( Order.class )
					.addAnnotatedClass( LineItem.class )
					.buildMetadata();

			final SessionFactory sf = bootModel.getSessionFactoryBuilder()
					.enableJpaOrderByMappingCompliance( complianceEnabled )
					.build();

			try {
				TransactionUtil2.inTransaction(
						(SessionFactoryImplementor) sf,
						session -> session.createQuery( "from Order" ).list()
				);
			}
			finally {
				try {
					sf.close();
				}
				catch (Exception ignore) {
				}
			}
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@Entity( name = "Order" )
	@Table( name = "orders" )
	public static class Order {
		@Id
		private Integer id;
		private String invoice;
		@OneToMany
		@OrderBy( "qty" )
		private Set<LineItem> lineItems;
	}

	@Entity( name = "LineItem" )
	@Table( name = "line_items" )
	public static class LineItem {
		@Id
		private Integer id;
		private String sku;
		@Column( name = "qty" )
		private int quantity;
	}
}

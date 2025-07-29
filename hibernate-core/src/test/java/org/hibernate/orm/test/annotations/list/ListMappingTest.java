/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.list;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Column;
import org.hibernate.tool.schema.internal.SchemaCreatorImpl;
import org.hibernate.tool.schema.internal.exec.GenerationTargetToStdout;


import org.hibernate.testing.util.ServiceRegistryUtil;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Originally developed to help diagnose HHH-10099 which reports a problem with @OrderColumn
 * not being bound
 *
 * @author Steve Ebersole
 */
public class ListMappingTest  {
	private StandardServiceRegistry ssr;

	@BeforeEach
	public void before() {
		ssr = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( AvailableSettings.FORMAT_SQL, false )
				.build();
	}

	@AfterEach
	public void after() {
		if ( ssr != null ) {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@Test
	public void testOrderColumnInNormalBiDirectonalModel() {
		Metadata metadata = new MetadataSources( ssr )
				.addAnnotatedClass( Order.class )
				.addAnnotatedClass( LineItem.class )
				.buildMetadata();

		Collection lineItemsBinding = metadata.getCollectionBindings().iterator().next();

		// make sure it was interpreted as a List (aka, as having an OrderColumn at all)
		assertThat( lineItemsBinding, instanceOf( org.hibernate.mapping.List.class ) );
		org.hibernate.mapping.List asList = (org.hibernate.mapping.List) lineItemsBinding;

		// assert the OrderColumn details
		final Column positionColumn = (Column) asList.getIndex().getSelectables().get( 0 );
		assertThat( positionColumn.getName(), equalTo( "position" ) );

		// make sure the OrderColumn is part of the collection table
		assertTrue( asList.getCollectionTable().containsColumn( positionColumn ) );

		class TargetImpl extends GenerationTargetToStdout {
			boolean found = false;
			@Override
			public void accept(String action) {
				super.accept( action );
				if ( action.matches( "^create( (column|row))? table t_line_item.+" ) ) {
					if ( action.contains( "position" ) ) {
						found = true;
					}
				}
			}
		}

		TargetImpl target = new TargetImpl();

		new SchemaCreatorImpl( ssr ).doCreation(
				metadata,
				true,
				target
		);

		assertTrue( target.found );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Bidirectional model

	@Entity( name = "Order" )
	@Table( name = "t_order" )
	public static class Order {
		private Integer id;
		private List<LineItem> lineItems = new ArrayList<LineItem>();

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		@OneToMany( mappedBy = "order" )
		@OrderColumn( name = "position" )
		public List<LineItem> getLineItems() {
			return lineItems;
		}

		public void setLineItems(List<LineItem> lineItems) {
			this.lineItems = lineItems;
		}
	}

	@Entity( name = "LineItem" )
	@Table( name = "t_line_item" )
	public static class LineItem {
		private Integer id;
		private Order order;
		private String product;
		private int quantity;
		private String discountCode;

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		@ManyToOne( optional = false )
		@JoinColumn( name = "order_id" )
		public Order getOrder() {
			return order;
		}

		public void setOrder(Order order) {
			this.order = order;
		}

		public String getProduct() {
			return product;
		}

		public void setProduct(String product) {
			this.product = product;
		}

		public int getQuantity() {
			return quantity;
		}

		public void setQuantity(int quantity) {
			this.quantity = quantity;
		}

		public String getDiscountCode() {
			return discountCode;
		}

		public void setDiscountCode(String discountCode) {
			this.discountCode = discountCode;
		}
	}
}

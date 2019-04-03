/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.tool.schemacreation.list;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;
import javax.persistence.Table;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Column;
import org.hibernate.orm.test.tool.BaseSchemaUnitTestCase;
import org.hibernate.tool.schema.internal.DefaultSchemaFilter;
import org.hibernate.tool.schema.internal.exec.GenerationTargetToStdout;

import org.hibernate.testing.junit5.schema.SchemaScope;
import org.hibernate.testing.junit5.schema.SchemaTest;

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
public class ListMappingTest extends BaseSchemaUnitTestCase {

	@Override
	protected void applySettings(StandardServiceRegistryBuilder serviceRegistryBuilder) {
		serviceRegistryBuilder.applySetting( AvailableSettings.FORMAT_SQL, false );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Order.class,
				LineItem.class
		};
	}

	@SchemaTest
	public void testOrderColumnInNormalBiDirectonalModel(SchemaScope schemaScope) {

		Collection lineItemsBinding = getMetadata().getCollectionBindings().iterator().next();

		// make sure it was interpreted as a List (aka, as having an OrderColumn at all)
		assertThat( lineItemsBinding, instanceOf( org.hibernate.mapping.List.class ) );
		org.hibernate.mapping.List asList = (org.hibernate.mapping.List) lineItemsBinding;

		// assert the OrderColumn details
		final Column positionColumn = (Column) asList.getIndex().getMappedColumns().get( 0 );
		assertThat( positionColumn.getName().getText(), equalTo( "position" ) );

		// make sure the OrderColumn is part of the collection table
		assertTrue( asList.getCollectionTable().containsColumn( positionColumn ) );

		TargetImpl target = new TargetImpl();
		schemaScope.withSchemaCreator(
				new DefaultSchemaFilter(),
				schemaCreator -> schemaCreator.doCreation( true, target )
		);

		assertTrue( target.found );
	}

	public static class TargetImpl extends GenerationTargetToStdout {
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

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Bi-directional model

	@Entity(name = "Order")
	@Table(name = "t_order")
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

		@OneToMany(mappedBy = "order")
		@OrderColumn(name = "position")
		public List<LineItem> getLineItems() {
			return lineItems;
		}

		public void setLineItems(List<LineItem> lineItems) {
			this.lineItems = lineItems;
		}
	}

	@Entity(name = "LineItem")
	@Table(name = "t_line_item")
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

		@ManyToOne(optional = false)
		@JoinColumn(name = "order_id")
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

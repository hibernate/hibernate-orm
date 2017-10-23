/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.metamodel;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
@Entity
@Table(name = "LINEITEM_TABLE")
public class LineItem implements java.io.Serializable {
	private String id;
	private int quantity;
	private Order order;
	private Product product;

	public LineItem() {
	}

	public LineItem(String v1, int v2, Order v3, Product v4) {
		id = v1;
		quantity = v2;
		order = v3;
		product = v4;
	}

	public LineItem(String v1, int v2) {
		id = v1;
		quantity = v2;
	}

	@Id
	@Column(name = "ID")
	public String getId() {
		return id;
	}

	public void setId(String v) {
		id = v;
	}

	@Column(name = "QUANTITY")
	public int getQuantity() {
		return quantity;
	}

	public void setQuantity(int v) {
		quantity = v;
	}

	@ManyToOne
	@JoinColumn(name = "FK1_FOR_ORDER_TABLE")
	public Order getOrder() {
		return order;
	}

	public void setOrder(Order v) {
		order = v;
	}

	@ManyToOne
	@JoinColumn(name = "FK_FOR_PRODUCT_TABLE")
	public Product getProduct() {
		return product;
	}

	public void setProduct(Product v) {
		product = v;
	}
}
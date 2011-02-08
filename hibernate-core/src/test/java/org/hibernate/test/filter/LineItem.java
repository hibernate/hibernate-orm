// $Id: LineItem.java 4046 2004-07-20 04:07:40Z steveebersole $
package org.hibernate.test.filter;


/**
 * Implementation of LineItem.
 * 
 * @author Steve
 */
public class LineItem {
	private Long id;
	private Order order;
	private int sequence;
	private Product product;
	private long quantity;

	/*package*/ LineItem() {}

	public static LineItem generate(Order order, int sequence, Product product, long quantity) {
		LineItem item = new LineItem();
		item.order = order;
		item.sequence = sequence;
		item.product = product;
		item.quantity = quantity;
		item.order.getLineItems().add(sequence, item);
		return item;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Order getOrder() {
		return order;
	}

	public void setOrder(Order order) {
		this.order = order;
	}

	public int getSequence() {
		return sequence;
	}

	public void setSequence(int sequence) {
		this.sequence = sequence;
	}

	public Product getProduct() {
		return product;
	}

	public void setProduct(Product product) {
		this.product = product;
	}

	public long getQuantity() {
		return quantity;
	}

	public void setQuantity(long quantity) {
		this.quantity = quantity;
	}
}

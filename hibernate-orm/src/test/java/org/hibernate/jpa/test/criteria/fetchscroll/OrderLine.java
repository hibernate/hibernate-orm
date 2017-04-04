
package org.hibernate.jpa.test.criteria.fetchscroll;

import javax.persistence.*;

@Entity
@Table(name = "order_lines")
public class OrderLine {

	private OrderLineId id;
	private Product product;
	private Order header;
	
	public OrderLine() {
	}
	
	public OrderLine(Order order, Long lineNumber, Product product) {
		this.id = new OrderLineId();
		this.id.setPurchaseOrgId(order.getId().getPurchaseOrgId());
		this.id.setNumber(order.getId().getNumber());
		this.id.setLineNumber(lineNumber);
		this.header = order;
		this.product = product;
	}
	
	@EmbeddedId
	public OrderLineId getId() {
		return id;
	}
	
	public void setId(OrderLineId id) {
		this.id = id;
	}
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumns({
		@JoinColumn(name = "FACILITY_ID", referencedColumnName = "FACILITY_ID", nullable = false, updatable = false),
		@JoinColumn(name = "PRODUCT_ID", referencedColumnName = "PRODUCT_ID", nullable = false, updatable = false)
	})
	public Product getProduct() {
		return product;
	}
	
	public void setProduct(Product product) {
		this.product = product;
	}
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumns({
		@JoinColumn(name = "PURCHASE_ORG_ID", referencedColumnName = "PURCHASE_ORG_ID", nullable = false, insertable = false, updatable = false),
		@JoinColumn(name = "ORDER_NUMBER", referencedColumnName = "ORDER_NUMBER", nullable = false, insertable = false, updatable = false)
	})
	public Order getHeader() {
		return header;
	}
	
	public void setHeader(Order header) {
		this.header = header;
	}
	
}


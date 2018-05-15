
package org.hibernate.jpa.test.criteria.fetchscroll;

import java.util.*;
import javax.persistence.*;

@Entity
@Table(name = "order_headers")
public class Order {

	private OrderId id;
	private PurchaseOrg purchaseOrg;
	private Set<OrderLine> lines;
	
	public Order() {
	
	}
	
	public Order(PurchaseOrg purchaseOrg, String number) {
		this.id = new OrderId();
		this.id.setPurchaseOrgId(purchaseOrg.getId());
		this.id.setNumber(number);
		this.purchaseOrg = purchaseOrg;
	}
	
	@EmbeddedId
	public OrderId getId() {
		return id;
	}
	
	public void setId(OrderId id) {
		this.id = id;
	}
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "PURCHASE_ORG_ID", referencedColumnName = "PURCHASE_ORG_ID", nullable = false, insertable = false, updatable = false)
	public PurchaseOrg getPurchaseOrg() {
		return purchaseOrg;
	}
	
	public void setPurchaseOrg(PurchaseOrg purchaseOrg) {
		this.purchaseOrg = purchaseOrg;
	}
	
	@OneToMany(mappedBy = "header", orphanRemoval = true, cascade = CascadeType.ALL)
	public Set<OrderLine> getLines() {
		return lines;
	}
	
	public void setLines(Set<OrderLine> lines) {
		this.lines = lines;
	}
	
}


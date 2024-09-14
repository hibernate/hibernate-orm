/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.criteria.fetchscroll;

import java.util.*;
import jakarta.persistence.*;

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

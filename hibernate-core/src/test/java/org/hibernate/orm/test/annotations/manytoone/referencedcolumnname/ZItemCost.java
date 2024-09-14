/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.manytoone.referencedcolumnname;
import java.math.BigDecimal;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class ZItemCost extends GenericObject {

	Item item;
	Vendor vendor;
	BigDecimal cost;

	@ManyToOne( fetch = FetchType.LAZY )
	@JoinColumn(name="item_id", nullable=false)
	public Item getItem() {
		return item;
	}

	public void setItem(Item item) {
		this.item = item;
	}

	@ManyToOne( fetch = FetchType.LAZY )
	@JoinColumn(name="vendor_id", nullable=false)
	public Vendor getVendor() {
		return vendor;
	}

	public void setVendor(Vendor vendor) {
		this.vendor = vendor;
	}

	public BigDecimal getCost() {
		return cost;
	}

	public void setCost(BigDecimal cost) {
		this.cost = cost;
	}
}

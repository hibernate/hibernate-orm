/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$
package org.hibernate.test.annotations.manytoone.referencedcolumnname;
import java.math.BigDecimal;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;

@Entity
public class WarehouseItem extends GenericObject {


	Item item;
	Vendor vendor;
	ZItemCost defaultCost;
	BigDecimal qtyInStock;


	public BigDecimal getQtyInStock() {
		return qtyInStock;
	}

	public void setQtyInStock(BigDecimal qtyInStock) {
		this.qtyInStock = qtyInStock;
	}

	@ManyToOne
//(fetch=FetchType.LAZY)
	@JoinColumn( name = "ITEM_ID", unique = false, nullable = false, insertable = true, updatable = true )
	public Item getItem() {
		return item;
	}

	public void setItem(Item item) {
		this.item = item;
	}

	@ManyToOne( fetch = FetchType.LAZY )
	@JoinColumn( name = "VENDOR_ID", unique = false, nullable = false, insertable = true, updatable = true )
	public Vendor getVendor() {
		return vendor;
	}

	public void setVendor(Vendor vendor) {
		this.vendor = vendor;
	}

	@ManyToOne
	@JoinColumns( {
	@JoinColumn( name = "vendor_id", referencedColumnName = "vendor_id", insertable = false, updatable = false ),
	@JoinColumn( name = "item_id", referencedColumnName = "item_id", insertable = false, updatable = false )
			} )
	public ZItemCost getDefaultCost() {
		return defaultCost;
	}

	public void setDefaultCost(ZItemCost defaultCost) {
		this.defaultCost = defaultCost;
	}
}

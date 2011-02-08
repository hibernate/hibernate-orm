//$
package org.hibernate.test.annotations.manytoone.referencedcolumnname;
import java.math.BigDecimal;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;

@Entity
public class ZItemCost extends GenericObject {

	Item item;
	Vendor vendor;
	BigDecimal cost;

	@ManyToOne( fetch = FetchType.LAZY )
	//@JoinColumn(name="ITEM_ID", unique=false, nullable=false, insertable=true, updatable=true)
	public Item getItem() {
		return item;
	}

	public void setItem(Item item) {
		this.item = item;
	}

	@ManyToOne( fetch = FetchType.LAZY )
	//@JoinColumn(name="VENDOR_ID", unique=false, nullable=false, insertable=true, updatable=true)
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


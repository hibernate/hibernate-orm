package org.hibernate.test.onetomany.inheritance.perclass;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;

import org.hibernate.test.onetomany.inheritance.Product;

@Entity
@Table(name="PRODTABPC")
@Access(AccessType.FIELD)
@Inheritance(strategy=InheritanceType.TABLE_PER_CLASS)
public abstract class ProductImpl implements Product {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue
	private int entid;

	@Column(name="INVCODE")
	private String inventoryCode;
	
	public ProductImpl() {
		
	}
	
	public ProductImpl(String inventoryCode) {
		this.inventoryCode = inventoryCode;
	}
	
	@Override
	public int getEntid() {
		return entid;
	}
	
	@Override
	public String getInventoryCode() {
		return inventoryCode;
	}
}

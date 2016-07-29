package org.hibernate.test.onetomany.inheritance.joined;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;

@Entity
@Table(name="PRODTABJO")
@Inheritance(strategy=InheritanceType.JOINED)
public abstract class Product {

	@Id
	@GeneratedValue
	private int entid;

	@Column(name="INVCODE")
	private String inventoryCode;
	
	public Product() {
		
	}
	
	public Product(String inventoryCode) {
		this.inventoryCode = inventoryCode;
	}

	public int getEntid() {
		return entid;
	}

	public String getInventoryCode() {
		return inventoryCode;
	}
}

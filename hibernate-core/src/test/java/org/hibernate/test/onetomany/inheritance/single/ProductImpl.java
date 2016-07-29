package org.hibernate.test.onetomany.inheritance.single;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.MappedSuperclass;
import javax.persistence.Table;

import org.hibernate.test.onetomany.inheritance.Product;

@Entity
@Table(name="PRODTABSG")
@Access(AccessType.FIELD)
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
public abstract class ProductImpl implements Product {
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

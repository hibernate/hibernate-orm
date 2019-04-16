package org.hibernate.test.instrument.domain;


import javax.persistence.Entity;
import javax.persistence.Table;

@Entity( name = "DomesticCustomer" )
@Table( name = "domestic_cust" )
public class DomesticCustomer extends Customer {
	private String taxId;

	public DomesticCustomer() {
	}

	public DomesticCustomer(
			Integer oid,
			String name,
			String taxId,
			Address address,
			Customer parentCustomer) {
		super(oid, name, address, parentCustomer);
		this.taxId = taxId;
	}

	public String getTaxId() {
		return taxId;
	}

	public void setTaxId(String taxId) {
		this.taxId = taxId;
	}
}

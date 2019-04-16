package org.hibernate.test.instrument.domain;


import javax.persistence.Entity;
import javax.persistence.Table;

@Entity( name = "ForeignCustomer" )
@Table( name = "foreign_cust" )
public class ForeignCustomer extends Customer {
	private String vat;

	public ForeignCustomer() {
	}

	public ForeignCustomer(
			Integer oid,
			String name,
			Address address,
			String vat,
			Customer parentCustomer) {
		super(oid, name, address, parentCustomer);
		this.vat = vat;
	}

	public ForeignCustomer(
			Integer oid,
			String name,
			Address address,
			String vat) {
		this( oid, name, address, vat, null );
	}

	public String getVat() {
		return vat;
	}

	public void setVat(String vat) {
		this.vat = vat;
	}
}

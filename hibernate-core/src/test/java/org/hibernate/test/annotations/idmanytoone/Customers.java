//$
package org.hibernate.test.annotations.idmanytoone;

import java.io.Serializable;
import javax.persistence.Id;
import javax.persistence.Column;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name="Customers")
@org.hibernate.annotations.Proxy(lazy=false)
public class Customers implements Serializable {

	private static final long serialVersionUID = -885167444315163039L;

	@Column(name="customerID", nullable=false)
	@Id
	private int customerID;

	@OneToMany(mappedBy="owner", cascade= CascadeType.ALL, targetEntity=ShoppingBaskets.class)
	@org.hibernate.annotations.LazyCollection(org.hibernate.annotations.LazyCollectionOption.TRUE)
	private java.util.Set shoppingBasketses = new java.util.HashSet();

	public void setCustomerID(int value) {
		this.customerID = value;
	}

	public int getCustomerID() {
		return customerID;
	}

	public int getORMID() {
		return getCustomerID();
	}

	public void setShoppingBasketses(java.util.Set value) {
		this.shoppingBasketses = value;
	}

	public java.util.Set getShoppingBasketses() {
		return shoppingBasketses;
	}

}


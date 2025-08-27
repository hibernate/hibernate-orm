/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.idmanytoone;
import java.io.Serializable;
import jakarta.persistence.Basic;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Embeddable
public class ShoppingBasketsPK implements Serializable {
	private static final long serialVersionUID = 4121297376338222776L;

	public boolean equals(Object aObj) {
		if (aObj == this)
			return true;
		if (!(aObj instanceof ShoppingBasketsPK))
			return false;
		ShoppingBasketsPK shoppingbasketspk = (ShoppingBasketsPK)aObj;
		if (getOwner() == null && shoppingbasketspk.getOwner() != null)
			return false;
		if (!getOwner().equals(shoppingbasketspk.getOwner()))
			return false;
		if (getBasketDatetime() != shoppingbasketspk.getBasketDatetime())
			return false;
		return true;
	}

	public int hashCode() {
		int hashcode = 0;
		if (getOwner() != null) {
			hashcode = hashcode + getOwner().getORMID();
		}
		hashcode = hashcode + (getBasketDatetime() == null ? 0 : getBasketDatetime().hashCode());
		return hashcode;
	}

	@Id
	@ManyToOne(cascade={ CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH })
	@JoinColumn(name="customerID", referencedColumnName="customerID")
	@Basic(fetch= FetchType.LAZY)
	private Customers owner;

	public void setOwner(Customers value)  {
		this.owner =  value;
	}

	public Customers getOwner()  {
		return this.owner;
	}

	@Column(name="basketDatetime", nullable=false)
	@Id
	private java.util.Date basketDatetime;

	public void setBasketDatetime(java.util.Date value)  {
		this.basketDatetime =  value;
	}

	public java.util.Date getBasketDatetime()  {
		return this.basketDatetime;
	}

}

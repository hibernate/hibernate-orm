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
public class BasketItemsPK implements Serializable {

	private static final long serialVersionUID = 3585214409096105241L;

	public boolean equals(Object aObj) {
		if (aObj == this)
			return true;
		if (!(aObj instanceof BasketItemsPK))
			return false;
		BasketItemsPK basketitemspk = (BasketItemsPK)aObj;
		if (getShoppingBaskets() == null && basketitemspk.getShoppingBaskets() != null)
			return false;
		if (!getShoppingBaskets().equals(basketitemspk.getShoppingBaskets()))
			return false;
		if ((getCost() != null && !getCost().equals(basketitemspk.getCost())) || (getCost() == null && basketitemspk.getCost() != null))
			return false;
		return true;
	}

	public int hashCode() {
		int hashcode = 0;
		if (getShoppingBaskets() != null) {
			hashcode = hashcode + (getShoppingBaskets().getOwner() == null ? 0 : getShoppingBaskets().getOwner().hashCode());
			hashcode = hashcode + (getShoppingBaskets().getBasketDatetime() == null ? 0 : getShoppingBaskets().getBasketDatetime().hashCode());
		}
		hashcode = hashcode + (getCost() == null ? 0 : getCost().hashCode());
		return hashcode;
	}

	@Id
	@ManyToOne(cascade={ CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH })
	@JoinColumn(name="basketDatetime", referencedColumnName="basketDatetime")
	@JoinColumn(name="customerID", referencedColumnName="customerID")
	@Basic(fetch= FetchType.LAZY)
	private ShoppingBaskets shoppingBaskets;

	public void setShoppingBaskets(ShoppingBaskets value)  {
		this.shoppingBaskets =  value;
	}

	public ShoppingBaskets getShoppingBaskets()  {
		return this.shoppingBaskets;
	}

	@Column(name="cost", nullable=false)
	@Id
	private Double cost;

	public void setCost(Double value)  {
		this.cost =  value;
	}

	public Double getCost()  {
		return this.cost;
	}

}

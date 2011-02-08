//$
package org.hibernate.test.annotations.idmanytoone;
import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;

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
	@JoinColumns({ @JoinColumn(name="basketDatetime", referencedColumnName="basketDatetime"), @JoinColumn(name="customerID", referencedColumnName="customerID") })
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


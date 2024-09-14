/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.idmanytoone;

import java.io.Serializable;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name="ShoppingBasket")
@IdClass(ShoppingBasketsPK.class)
public class ShoppingBaskets implements Serializable {

	private static final long serialVersionUID = 4739240471638885734L;

	@Id
	@ManyToOne(cascade={ CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH })
	@JoinColumn(name="customerID", referencedColumnName="customerID")
	private Customers owner;

	@Column(name="basketDatetime", nullable=false)
	@Id
	private java.util.Date basketDatetime;

	@OneToMany(mappedBy="shoppingBaskets", cascade=CascadeType.ALL, targetEntity=BasketItems.class)
	private java.util.Set items = new java.util.HashSet();

	public void setBasketDatetime(java.util.Date value) {
		this.basketDatetime = value;
	}

	public java.util.Date getBasketDatetime() {
		return basketDatetime;
	}

	public void setOwner(Customers value) {
		this.owner = value;
	}

	public Customers getOwner() {
		return owner;
	}

	public void setItems(java.util.Set value) {
		this.items = value;
	}

	public java.util.Set getItems() {
		return items;
	}

}

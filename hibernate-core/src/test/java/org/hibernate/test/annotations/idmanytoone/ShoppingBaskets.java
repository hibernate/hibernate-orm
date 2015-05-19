/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$
package org.hibernate.test.annotations.idmanytoone;
import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name="ShoppingBasket")
@org.hibernate.annotations.Proxy(lazy=false)
@IdClass(ShoppingBasketsPK.class)
public class ShoppingBaskets implements Serializable {

	private static final long serialVersionUID = 4739240471638885734L;

	@Id
	@ManyToOne(cascade={ CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH })
	@JoinColumns({ @JoinColumn(name="customerID", referencedColumnName="customerID") })
	@Basic(fetch=FetchType.LAZY)
	private Customers owner;

	@Column(name="basketDatetime", nullable=false)
	@Id
	private java.util.Date basketDatetime;

	@OneToMany(mappedBy="shoppingBaskets", cascade=CascadeType.ALL, targetEntity=BasketItems.class)
	@org.hibernate.annotations.LazyCollection(org.hibernate.annotations.LazyCollectionOption.TRUE)
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


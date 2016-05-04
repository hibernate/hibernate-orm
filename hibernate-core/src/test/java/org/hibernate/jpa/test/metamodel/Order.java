/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.metamodel;
import java.util.Collection;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
@Entity
@Table(name = "ORDER_TABLE")
public class Order implements java.io.Serializable {
	private String id;
	private double totalPrice;
	private Customer customer;
	private CreditCard creditCard;
	private LineItem sampleLineItem;
	private Collection<LineItem> lineItems = new java.util.ArrayList<LineItem>();

	private char[] domen;
	private byte[] number;

	public Order() {
	}

	public Order(String id, double totalPrice) {
		this.id = id;
		this.totalPrice = totalPrice;
	}

	public Order(String id, Customer customer) {
		this.id = id;
		this.customer = customer;
	}

	public Order(String id, char[] domen) {
		this.id = id;
		this.domen = domen;
	}


	public Order(String id) {
		this.id = id;
	}

	//====================================================================
	// getters and setters for State fields

	@Id
	@Column(name = "ID")
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Column(name = "TOTALPRICE")
	public double getTotalPrice() {
		return totalPrice;
	}

	public void setTotalPrice(double price) {
		this.totalPrice = price;
	}

	//====================================================================
	// getters and setters for Association fields

	// MANYx1

	@ManyToOne
	@JoinColumn(
			name = "FK4_FOR_CUSTOMER_TABLE")
	public Customer getCustomer() {
		return customer;
	}

	public void setCustomer(Customer customer) {
		this.customer = customer;
	}

	//1x1

	@OneToOne(mappedBy = "order")
	public CreditCard getCreditCard() {
		return creditCard;
	}

	public void setCreditCard(CreditCard cc) {
		this.creditCard = cc;
	}

	// 1x1

	@OneToOne(cascade = CascadeType.REMOVE)
	@JoinColumn(
			name = "FK0_FOR_LINEITEM_TABLE")
	public LineItem getSampleLineItem() {
		return sampleLineItem;
	}

	public void setSampleLineItem(LineItem l) {
		this.sampleLineItem = l;
	}

	//1xMANY

	@OneToMany(cascade = CascadeType.ALL, mappedBy = "order")
	public Collection<LineItem> getLineItems() {
		return lineItems;
	}

	public void setLineItems(Collection<LineItem> c) {
		this.lineItems = c;
	}

	public char[] getDomen() {
		return domen;
	}

	public void setDomen(char[] d) {
		domen = d;
	}

	@Column(name="fld_number")
	public byte[] getNumber() {
		return number;
	}

	public void setNumber(byte[] n) {
		number = n;
	}


}

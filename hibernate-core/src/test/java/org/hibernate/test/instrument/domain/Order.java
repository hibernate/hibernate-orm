package org.hibernate.test.instrument.domain;

import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import java.util.HashSet;
import java.util.Set;

@Entity( name = "Order" )
@Table( name = "`order`" )
@Inheritance( strategy = InheritanceType.TABLE_PER_CLASS )
public class Order {
	private Integer oid;

	private String theText;

	private Customer customer;
	private OrderSupplemental supplemental;
	private OrderSupplemental2 supplemental2;

	private Set<Payment> payments = new HashSet<Payment>();

	public Order() {
	}

	public Order(Integer oid, String theText, Customer customer) {
		this.oid = oid;
		this.theText = theText;
		this.customer = customer;
	}

	@Id
	@Column( name = "oid" )
	public Integer getOid() {
		return oid;
	}

	public void setOid(Integer oid) {
		this.oid = oid;
	}

	public String getTheText() {
		return theText;
	}

	public void setTheText(String theText) {
		this.theText = theText;
	}

	@ManyToOne( fetch = FetchType.LAZY )
	@JoinColumn
//	@LazyToOne( LazyToOneOption.NO_PROXY )
	public Customer getCustomer() {
		return customer;
	}

	public void setCustomer(Customer customer) {
		this.customer = customer;
	}

	@OneToMany( fetch = FetchType.LAZY )
	public Set<Payment> getPayments() {
		return payments;
	}

	public void setPayments(Set<Payment> payments) {
		this.payments = payments;
	}

	@OneToOne( fetch = FetchType.LAZY )
	@JoinColumn( name = "supp_info_id" )
	public OrderSupplemental getSupplemental() {
		return supplemental;
	}

	public void setSupplemental(OrderSupplemental supplemental) {
		this.supplemental = supplemental;
	}

	@OneToOne( fetch = FetchType.LAZY, mappedBy = "order" )
	@LazyToOne( LazyToOneOption.NO_PROXY )
	public OrderSupplemental2 getSupplemental2() {
		return supplemental2;
	}

	public void setSupplemental2(OrderSupplemental2 supplemental2) {
		this.supplemental2 = supplemental2;
	}
}

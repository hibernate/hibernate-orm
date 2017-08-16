package org.hibernate.test.mapping.cascade;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.annotations.AccessType;

/**
 * Receipt as an example for a component
 */
@Entity
@Table(name = "OTM_Receipt")
public class Receipt {

	@OneToOne(cascade = { CascadeType.PERSIST})
	private Invoice invoice;
	
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    @AccessType("property")
    private Long id;

	@Column(length = 50, nullable = false)
	private String name;

	/**
	 * Constructor
	 */
	public Receipt() {
		super();
	}

	/**
	 * Constructor
	 * 
	 * @param name
	 */
	public Receipt(String name) {
		super();
		this.name = name;
	}
	
	/**
	 * @return invoice
	 */
	public Invoice getInvoice() {
		return invoice;
	}
	
	/**
	 * @return invoice
	 */	
	public void setInvoice(Invoice invoice) {
		this.invoice = invoice;
	}

	/**
	 * @return id
	 */
	public Long getId() {
		return id;
	}

	/**
	 * @param id
	 */
	public void setId(Long id) {
		this.id = id;
	}

}

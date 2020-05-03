package org.hibernate.test.bytecode.enhancement.lazy.group;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.AccessType;

/**
 * Target of a LazyToOne - relationship (Foreignkey on this side)
 *
 * @author Jan-Oliver Lustig, Sebastian Viefhaus
 */
@Entity
@Table(name = "LGMB_TO")
public class LGMB_To {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(nullable = false)
	@AccessType("property")
	private Long id;

	@OneToOne
	LGMB_From fromRelation;

	@Column(length = 50, nullable = false)
	private String name;

	/**
	 * Default Constructor
	 */
	public LGMB_To() {
		super();
	}

	/**
	 * Constructor
	 */
	public LGMB_To(String name) {
		super();
		this.name = name;
	}

	/**
	 * @return the fromRelation
	 */
	public LGMB_From getFromRelation() {
		return fromRelation;
	}

	/**
	 * @param fromRelation the fromRelation to set
	 */
	public void setFromRelation(LGMB_From fromRelation) {
		this.fromRelation = fromRelation;
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

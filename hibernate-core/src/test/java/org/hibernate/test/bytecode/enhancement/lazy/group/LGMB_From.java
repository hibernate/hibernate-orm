package org.hibernate.test.bytecode.enhancement.lazy.group;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;

import org.hibernate.annotations.LazyGroup;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;

/**
 * Source of a LazyToOne - relationship with FK on the other side
 *
 * @author Jan-Oliver Lustig, Sebastian Viefhaus
 */
@Entity
@Access(AccessType.FIELD)
public class LGMB_From {

	@Column(length = 50, nullable = false)
	private String name;

	// Lazy-Attribut without LazyGroup-Annotation (therefore Default-LazyGroup)
	@Column(length = 65000, columnDefinition = "text")
	@Basic(fetch = FetchType.LAZY)
	private String bigText;

	// Lazy-Assoziation with mappdedBy in own LazyGroup
	@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, mappedBy = "fromRelation", optional = true)
	@LazyToOne(LazyToOneOption.NO_PROXY)
	@LazyGroup(value = "toRelationLazyGroup")
	private LGMB_To toRelation;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(nullable = false)
	private Long id;

	/**
	 * Default Constructor
	 */
	public LGMB_From() {
		super();
	}

	/**
	 * Constructor
	 */
	public LGMB_From(String name) {
		super();
		this.name = name;
	}

	/**
	 * @return the toRelation
	 */
	public LGMB_To getToRelation() {
		return toRelation;
	}

	/**
	 * @param toRelation the toRelation to set
	 */
	public void setToRelation(LGMB_To toRelation) {
		this.toRelation = toRelation;
	}

	public String getBigText() {
		return bigText;
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

	public String getName() {
		return name;
	}

}

package org.hibernate.envers.test.integration.onetomany.bidirectional;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;

import org.hibernate.annotations.AccessType;

@MappedSuperclass
public class DomainObject {

	/**
	 * Id
	 */
	@Id
	@AccessType("property")
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	/** domain */
	@ManyToOne
	private Domain domain;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Domain getDomain() {
		return domain;
	}

	public void setDomain(Domain domain) {
		this.domain = domain;
	}
	
	
}

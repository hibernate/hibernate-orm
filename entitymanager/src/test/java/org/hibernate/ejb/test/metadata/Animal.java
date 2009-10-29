package org.hibernate.ejb.test.metadata;

import javax.persistence.MappedSuperclass;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;

/**
 * @author Emmanuel Bernard
 */
@MappedSuperclass
public class Animal extends SubThing {
	private Long id;
	private int legNbr;

	@Id @GeneratedValue
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public int getLegNbr() {
		return legNbr;
	}

	public void setLegNbr(int legNbr) {
		this.legNbr = legNbr;
	}
}

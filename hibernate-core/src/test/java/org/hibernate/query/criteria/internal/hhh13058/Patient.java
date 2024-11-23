package org.hibernate.query.criteria.internal.hhh13058;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * @author Archie Cobbs
 * @author Nathan Xu
 */
@Entity(name = "Patient")
@Table(name = "Patient")
public class Patient {

	@Id
	@GeneratedValue
	Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	Site site;

	public Patient() {
	}

	public Patient(Site site) {
		this.site = site;
	}

}

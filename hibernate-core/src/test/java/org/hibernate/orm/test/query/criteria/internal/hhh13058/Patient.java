/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.query.criteria.internal.hhh13058;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

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

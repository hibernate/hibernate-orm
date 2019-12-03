/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.loadplans.process.inheritance;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

/**
 * @author Steve Ebersole
 */
@Entity
public class Contractor extends User {

	// illustrates the problematic situation described in HHH-8330

	@ManyToOne(fetch= FetchType.EAGER)
	@JoinColumn(name="reportsTo_id",nullable=false)
	Department reportsTo;

	public Contractor(Integer id, Department reportsTo) {
		super( id );
		this.reportsTo = reportsTo;
	}

	public Contractor() {
	}
}

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.naturalid;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.NaturalIdCache;

@Entity
@NaturalIdCache
/**
 * Test case for NaturalId annotation - ANN-750
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
class NaturalIdOnManyToOne {

    @Id
    @GeneratedValue
    int id;

    @NaturalId
    @ManyToOne
    Citizen citizen;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Citizen getCitizen() {
		return citizen;
	}

	public void setCitizen(Citizen citizen) {
		this.citizen = citizen;
	}
}

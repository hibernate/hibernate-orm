/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.jpa.test.association;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Oven {
	@Id @GeneratedValue
	private Long id;

	@OneToOne(fetch= FetchType.LAZY)
	@JoinColumn
	private Kitchen kitchen;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Kitchen getKitchen() {
		return kitchen;
	}

	public void setKitchen(Kitchen kitchen) {
		this.kitchen = kitchen;
	}
}

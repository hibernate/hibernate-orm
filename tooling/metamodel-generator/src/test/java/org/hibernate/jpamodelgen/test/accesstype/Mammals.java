/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.test.accesstype;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Mammals extends LivingBeing {
	private String id;
	private String nbrOfMammals;

	@Id
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getNbrOfMammals() {
		return nbrOfMammals;
	}

	public void setNbrOfMammals(String nbrOfMammals) {
		this.nbrOfMammals = nbrOfMammals;
	}
}

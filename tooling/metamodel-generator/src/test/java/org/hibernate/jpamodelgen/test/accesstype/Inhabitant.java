/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.test.accesstype;

import java.util.Set;
import javax.persistence.Embeddable;
import javax.persistence.Access;
import javax.persistence.ElementCollection;

/**
 * @author Emmanuel Bernard
 */
@Embeddable
@Access(javax.persistence.AccessType.FIELD)
public class Inhabitant {
	private String name;
	@ElementCollection
	private Set<Pet> pets;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}

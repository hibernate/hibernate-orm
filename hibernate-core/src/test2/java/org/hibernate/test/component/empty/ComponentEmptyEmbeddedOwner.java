/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.component.empty;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
public class ComponentEmptyEmbeddedOwner {

	@Id
	@GeneratedValue
	private Integer id;

	private ComponentEmptyEmbedded embedded;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public ComponentEmptyEmbedded getEmbedded() {
		return embedded;
	}

	public void setEmbedded(ComponentEmptyEmbedded embedded) {
		this.embedded = embedded;
	}

}

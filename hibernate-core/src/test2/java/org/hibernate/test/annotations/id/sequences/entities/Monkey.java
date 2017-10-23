/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$
package org.hibernate.test.annotations.id.sequences.entities;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * @author Paul Cowan
 */
@Entity
public class Monkey {
	private String id;

	@Id
	@GeneratedValue(generator = "system-uuid-2")
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
}

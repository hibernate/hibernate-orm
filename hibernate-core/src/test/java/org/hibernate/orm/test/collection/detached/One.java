/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.collection.detached;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.List;

@Entity @Table(name="DCOne")
public class One {
	@GeneratedValue
	@Id
	long id;

	@OneToMany(mappedBy = "one",
			cascade = {CascadeType.PERSIST, CascadeType.MERGE})
	List<Many> many;

	public long getId() {
		return id;
	}
}

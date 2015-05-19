/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.eviction;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * @author Steve Ebersole
 */
@Entity
public class IsolatedEvictableEntity {
	@Id
	private Integer id;
	private String name;

	public IsolatedEvictableEntity() {
	}

	public IsolatedEvictableEntity(Integer id) {
		this.id = id;
	}
}

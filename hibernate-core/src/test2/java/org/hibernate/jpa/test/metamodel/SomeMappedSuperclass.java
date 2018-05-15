/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.metamodel;

import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

/**
 * @author Steve Ebersole
 */
@MappedSuperclass
public class SomeMappedSuperclass {
	private Long id;

	@Id
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
}

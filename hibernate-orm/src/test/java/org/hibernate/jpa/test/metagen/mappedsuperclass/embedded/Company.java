/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.metagen.mappedsuperclass.embedded;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * @author Steve Ebersole
 */
@Entity
public class Company extends AbstractAddressable {
	private Long id;

	public Company() {
	}

	public Company(Address address) {
		super( address );
	}

	@Id
	public Long getId() {
		return id;
	}

	private void setId(Long id) {
		this.id = id;
	}
}

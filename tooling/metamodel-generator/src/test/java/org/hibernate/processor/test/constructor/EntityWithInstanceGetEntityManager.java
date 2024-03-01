/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.processor.test.constructor;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;

@Entity
public class EntityWithInstanceGetEntityManager {

	@Transient
	public EntityManager getEntityManager() {
		// In a real-world scenario, this would contain some framework-specific code
		throw new IllegalStateException( "This method shouldn't be called in tests" );
	}

	@Id
	private Long id;
	private String name;
	private String entityManager;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}

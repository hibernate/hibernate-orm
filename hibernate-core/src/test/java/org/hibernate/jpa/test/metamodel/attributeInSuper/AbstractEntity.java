/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.metamodel.attributeInSuper;

import jakarta.persistence.Embedded;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

/**
 * @author Hardy Ferentschik
 */
@MappedSuperclass
public class AbstractEntity {
	@Id
	private long id;

	@Embedded
	private EmbeddableEntity embedded;

	public long getId() {
		return id;
	}

	public EmbeddableEntity getEmbedded() {
		return embedded;
	}
}

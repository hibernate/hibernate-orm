/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.querycache;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;

@Entity
public class EntityWithCompositeKey {

	@EmbeddedId
	public CompositeKey pk;

	public EntityWithCompositeKey() {
	}

	public EntityWithCompositeKey(CompositeKey pk) {
		this.pk = pk;
	}

}

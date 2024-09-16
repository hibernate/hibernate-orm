/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.processor.test.hhh17661;

import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class TreeRelation<T extends Tree<T, ? extends TreeRelation<T>>> extends Entity {

	@ManyToOne(optional = false)
	private T parent;

	@ManyToOne(optional = false)
	private T child;
}

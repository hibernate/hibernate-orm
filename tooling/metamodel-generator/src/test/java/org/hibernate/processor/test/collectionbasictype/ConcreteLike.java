/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.processor.test.collectionbasictype;

import jakarta.persistence.Entity;

@Entity(name = "ConcreteLike")
public class ConcreteLike extends Like<ConcreteLike.Target> {

	@Override
	public Reference<Target> getObject() {
		return new Reference<>();
	}

	public static class Target implements Like.I1, Like.I2 {
	}
}

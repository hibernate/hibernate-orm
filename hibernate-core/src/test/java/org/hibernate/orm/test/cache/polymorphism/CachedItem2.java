/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.cache.polymorphism;

import jakarta.persistence.Entity;


/**
 * @author Guillaume Smet
 */
@Entity
public class CachedItem2 extends Cacheable {

	public CachedItem2() {
		super();
	}

	public CachedItem2(long id, String name) {
		super( id, name );
	}

}

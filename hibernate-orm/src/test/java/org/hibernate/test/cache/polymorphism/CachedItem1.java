/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache.polymorphism;

import javax.persistence.Entity;


/**
 * @author Guillaume Smet
 */
@Entity
public class CachedItem1 extends AbstractCachedItem {

	public CachedItem1() {
		super();
	}

	public CachedItem1(String name) {
		super( name );
	}
	
}

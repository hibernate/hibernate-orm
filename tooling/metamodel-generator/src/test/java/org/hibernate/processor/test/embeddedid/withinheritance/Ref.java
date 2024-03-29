/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.processor.test.embeddedid.withinheritance;

/**
 * @author Hardy Ferentschik
 */
public class Ref extends AbstractRef {
	public Ref() {
	}

	public Ref(int id) {
		super( id );
	}
}



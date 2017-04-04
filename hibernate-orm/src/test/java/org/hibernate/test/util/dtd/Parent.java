/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.util.dtd;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * The Parent class.
 *
 * @author Steve Ebersole
 */
public class Parent {
	private Long id;
	private Set children = new HashSet();

	public Long getId() {
		return id;
	}

	public Iterator getChildren() {
		return children.iterator();
	}

	public Child newChild() {
		Child child = new Child();
		child.setAge( 0 );

		child.injectParent( this );
		this.children.add( child );

		return child;
	}
}

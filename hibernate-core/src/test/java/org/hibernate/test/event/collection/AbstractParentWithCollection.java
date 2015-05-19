/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: $

package org.hibernate.test.event.collection;
import java.util.Collection;

/**
 *
 * @author Gail Badner
 */
public abstract class AbstractParentWithCollection implements ParentWithCollection {
	private Long id;
	private String name;
	           
	private Collection children;

	public AbstractParentWithCollection() {
	}

	public AbstractParentWithCollection(String name) {
		this.name = name;
	}

	public void newChildren(Collection collection) {
		setChildren( collection );
	}

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

	public Collection getChildren() {
		return children;
	}

	public void setChildren(Collection children) {
		this.children = children;
	}

	public Child addChild(String name) {
		Child child = createChild( name );
		addChild( child );
		return child;
	}

	public void addChild(Child child) {
		if ( child != null ) {
			children.add( child );
		}
	}

	public void addAllChildren(Collection children) {
		this.children.addAll( children );
	}

	public void removeChild(Child child) {
		children.remove( child );
	}

	public void removeAllChildren(Collection children) {
		children.removeAll( children );
	}

	public void clearChildren() {
		if ( children != null && !children.isEmpty() ) {
			this.children.clear();
		}
	}
}

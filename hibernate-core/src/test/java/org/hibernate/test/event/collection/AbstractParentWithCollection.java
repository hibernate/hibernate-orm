//$Id: $
/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2007, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution statements
 * applied by the authors.
 *
 * All third-party contributions are distributed under license by Red Hat
 * Middleware LLC.  This copyrighted material is made available to anyone
 * wishing to use, modify, copy, or redistribute it subject to the terms
 * and conditions of the GNU Lesser General Public License, as published by
 * the Free Software Foundation.  This program is distributed in the hope
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See the GNU Lesser General Public License for more details.  You should
 * have received a copy of the GNU Lesser General Public License along with
 * this distribution; if not, write to: Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor Boston, MA  02110-1301  USA
 */
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

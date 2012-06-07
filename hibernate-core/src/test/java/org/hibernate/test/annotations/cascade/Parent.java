/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.annotations.cascade;

import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

/**
 * @author Jeff Schnitzer
 */
@Entity
public class Parent
{
	/** */
	@Id
	@GeneratedValue
	public Long id;

	/** */
	@OneToMany(cascade=CascadeType.ALL, mappedBy="parent")
	public Set<Child> children;
	
	/** */
	@OneToOne(cascade=CascadeType.ALL)
	@JoinColumn(name="defaultChildId", nullable=false)
	Child defaultChild;
	
	/** */
	public Parent() {}

	/** */
	public Child getDefaultChild() { return this.defaultChild; }
	public void setDefaultChild(Child value) { this.defaultChild = value; }
	
	/** */
	public Set<Child> getChildren() { return this.children; }
	public void setChildren(Set<Child> value) { this.children = value; }
}

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.cascade;

import java.util.Set;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

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

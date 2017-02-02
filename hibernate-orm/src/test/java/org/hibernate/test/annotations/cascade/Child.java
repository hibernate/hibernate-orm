/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.cascade;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

/**
 * @author Jeff Schnitzer
 */
@Entity
public class Child
{
	/** */
	@Id
	@GeneratedValue
	public Long id;

	/** */
	@ManyToOne(cascade=CascadeType.PERSIST)
	@JoinColumn(name="parentId", nullable=false)
	Parent parent;
	
	/** */
	public Child() {}
	
	/** */
	public Child(Parent p)
	{
		this.parent = p;
	}
	
	/** */
	public Parent getParent() { return this.parent; }
	public void setParent(Parent value) { this.parent = value; }
}

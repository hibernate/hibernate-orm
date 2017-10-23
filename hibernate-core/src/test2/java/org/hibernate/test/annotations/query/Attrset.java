/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.query;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;

@Entity
public class Attrset {
	@Id
	@GeneratedValue
	private Long id;
	
	@OneToMany
	@JoinTable(name = "ATTRSET_X_ATTRVALUE")
	private Set<Attrvalue> attrvalues = new HashSet<Attrvalue>();

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Set<Attrvalue> getAttrvalues() {
		return attrvalues;
	}

	public void setAttrvalues(Set<Attrvalue> attrvalues) {
		this.attrvalues = attrvalues;
	}
}

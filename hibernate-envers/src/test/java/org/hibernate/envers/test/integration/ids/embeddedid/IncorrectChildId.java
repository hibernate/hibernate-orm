/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.ids.embeddedid;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;

/**
 * @author Chris Cranford
 */
@Embeddable
public class IncorrectChildId implements Serializable {
	@ManyToOne
	@JoinColumns({ @JoinColumn(name = "parent_id", referencedColumnName = "id") })
	private Parent parent;

	@Column(name = "child_number")
	private Integer number;

	IncorrectChildId() {

	}

	public IncorrectChildId(Integer number, Parent parent) {
		this.number = number;
		this.parent = parent;
	}

	public Parent getParent() {
		return parent;
	}

	public void setParent(Parent parent) {
		this.parent = parent;
	}

	public Integer getNumber() {
		return number;
	}

	public void setNumber(Integer number) {
		this.number = number;
	}
}

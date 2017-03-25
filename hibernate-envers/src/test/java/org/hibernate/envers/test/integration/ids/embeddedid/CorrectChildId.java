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

/**
 * @author Chris Cranford
 */
@Embeddable
public class CorrectChildId implements Serializable {
	@Column(name = "parent_id")
	private String id;

	@Column(name = "child_number")
	private Integer number;

	CorrectChildId() {

	}

	public CorrectChildId(Integer number, Parent parent) {
		this.number = number;
		this.id = parent.getId();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Integer getNumber() {
		return number;
	}

	public void setNumber(Integer number) {
		this.number = number;
	}
}

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.ids.embeddedid;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;

import org.hibernate.envers.Audited;

/**
 * @author Chris Cranford
 */
@Audited
@Entity
public class CorrectChild {
	@EmbeddedId
	private CorrectChildId id;

	@ManyToOne
	@MapsId("parent_id")
	private Parent parent;

	CorrectChild() {

	}

	public CorrectChild(Integer number, Parent parent) {
		this.id = new CorrectChildId(number, parent);
		this.parent = parent;
	}

	public CorrectChildId getId() {
		return id;
	}

	public void setId(CorrectChildId id) {
		this.id = id;
	}

	public Parent getParent() {
		return parent;
	}

	public void setParent(Parent parent) {
		this.parent = parent;
	}
}

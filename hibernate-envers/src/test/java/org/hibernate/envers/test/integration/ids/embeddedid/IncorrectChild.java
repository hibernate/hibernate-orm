/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.ids.embeddedid;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;

import org.hibernate.envers.Audited;

/**
 * @author Chris Cranford
 */
@Audited
@Entity
public class IncorrectChild {
	@EmbeddedId
	private IncorrectChildId id;

	IncorrectChild() {

	}

	public IncorrectChild(Integer number, Parent parent) {
		this.id = new IncorrectChildId( number, parent );
	}

	public IncorrectChildId getId() {
		return id;
	}

	public void setId(IncorrectChildId id) {
		this.id = id;
	}
}

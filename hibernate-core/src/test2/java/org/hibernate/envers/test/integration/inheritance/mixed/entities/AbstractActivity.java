/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.inheritance.mixed.entities;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

import org.hibernate.envers.Audited;

@Audited
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public abstract class AbstractActivity implements Activity {
	@EmbeddedId
	private ActivityId id;

	private Integer sequenceNumber;

	public ActivityId getId() {
		return id;
	}

	public void setId(ActivityId id) {
		this.id = id;
	}

	public Integer getSequenceNumber() {
		return sequenceNumber;
	}

	public void setSequenceNumber(Integer sequenceNumber) {
		this.sequenceNumber = sequenceNumber;
	}
}

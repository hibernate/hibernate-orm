/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.entities.reventity.trackmodifiedentities;

import javax.persistence.Column;
import javax.persistence.Entity;

import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.enhanced.SequenceIdTrackingModifiedEntitiesRevisionEntity;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Entity
@RevisionEntity(ExtendedRevisionListener.class)
public class ExtendedRevisionEntity extends SequenceIdTrackingModifiedEntitiesRevisionEntity {
	@Column(name = "USER_COMMENT")
	private String comment;

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}
}

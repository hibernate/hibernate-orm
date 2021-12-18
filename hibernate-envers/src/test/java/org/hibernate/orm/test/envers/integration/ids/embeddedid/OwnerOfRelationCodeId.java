/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.envers.integration.ids.embeddedid;

import java.io.Serializable;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;

@Embeddable
public class OwnerOfRelationCodeId implements Serializable {

	@Embedded
	private CompositeEntityId compositeEntity;

	private String secondIdentifier;

	public CompositeEntityId getCompositeEntity() {
		return compositeEntity;
	}

	public void setCompositeEntity(CompositeEntityId compositeEntity) {
		this.compositeEntity = compositeEntity;
	}

	public String getSecondIdentifier() {
		return secondIdentifier;
	}

	public void setSecondIdentifier(String secondIdentifier) {
		this.secondIdentifier = secondIdentifier;
	}

	@Override
	public String toString() {
		return "OwnerOfRelationCodeId{" +
				"compositeEntity=" + compositeEntity +
				", secondIdentifier='" + secondIdentifier + '\'' +
				'}';
	}
}

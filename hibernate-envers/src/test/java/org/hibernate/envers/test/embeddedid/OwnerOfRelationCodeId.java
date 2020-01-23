package org.hibernate.envers.test.embeddedid;

import java.io.Serializable;

import javax.persistence.Embeddable;
import javax.persistence.Embedded;

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

}

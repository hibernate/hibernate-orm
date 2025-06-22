/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.ids.embeddedid;

import java.util.Set;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.envers.Audited;

@Entity
@Audited
@Table(name = "compositeentity")
@Access(value = AccessType.FIELD)
public class CompositeEntity {

	@EmbeddedId
	private CompositeEntityId codeObject = new CompositeEntityId();

	@OneToMany(targetEntity = OwnerOfRelationCode.class, fetch = FetchType.LAZY, mappedBy = "compositeEntity")
	private Set<OwnerOfRelationCode> ownerOfRelationCodes = new java.util.HashSet<>();

	public CompositeEntityId getCodeObject() {
		return codeObject;
	}

	public void setCodeObject(CompositeEntityId codeObject) {
		this.codeObject = codeObject;
	}

	public Set<OwnerOfRelationCode> getOwnerOfRelationCodes() {
		return ownerOfRelationCodes;
	}

	public void setOwnerOfRelationCodes(Set<OwnerOfRelationCode> ownerOfRelationCodes) {
		this.ownerOfRelationCodes = ownerOfRelationCodes;
	}

	public String getFirstCode() {
		return codeObject == null ? null : codeObject.getFirstCode();
	}

	public void setFirstCode(String firstCode) {
		if ( codeObject == null ) {
			codeObject = new CompositeEntityId();
		}
		codeObject.setFirstCode( firstCode );
	}

	public String getSecondCode() {
		return codeObject == null ? null : codeObject.getSecondCode();
	}

	public void setSecondCode(String secondCode) {
		if ( codeObject == null ) {
			codeObject = new CompositeEntityId();
		}
		codeObject.setSecondCode( secondCode );
	}

}

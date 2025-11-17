/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.ids.embeddedid;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

import org.hibernate.envers.Audited;

@Entity
@Audited
@Table(name = "ownerOfRelationcode")
@Access(value = AccessType.FIELD)
public class OwnerOfRelationCode {

	@EmbeddedId
	private OwnerOfRelationCodeId codeObject = new OwnerOfRelationCodeId();

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumns({
			@JoinColumn(name = "compositeEntity_firstCode", referencedColumnName = "firstCode", unique = false, nullable = false),
			@JoinColumn(name = "compositeEntity_secondCode", referencedColumnName = "secondCode", unique = false, nullable = false) })
	@MapsId("compositeEntity")
	private CompositeEntity compositeEntity;

	private String description;

	public OwnerOfRelationCodeId getCodeObject() {
		return codeObject;
	}

	public CompositeEntity getCompositeEntity() {
		return compositeEntity;
	}

	public void setCompositeEntity(CompositeEntity compositeEntity) {
		if ( codeObject == null ) {
			codeObject = new OwnerOfRelationCodeId();
		}
		if ( compositeEntity != null ) {
			codeObject.setCompositeEntity( compositeEntity.getCodeObject() );
		}
		this.compositeEntity = compositeEntity;
	}

	public String getSecondIdentifier() {
		return codeObject == null ? null : codeObject.getSecondIdentifier();

	}

	public void setSecondIdentifier(String secondIdentifier) {
		if ( codeObject == null ) {
			codeObject = new OwnerOfRelationCodeId();
		}
		codeObject.setSecondIdentifier( secondIdentifier );
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public String toString() {
		return "OwnerOfRelationCode{" +
				"codeObject=" + codeObject +
				", compositeEntity=" + compositeEntity +
				", description='" + description + '\'' +
				'}';
	}
}

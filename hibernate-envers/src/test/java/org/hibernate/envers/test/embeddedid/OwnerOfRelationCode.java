package org.hibernate.envers.test.embeddedid;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.Table;

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

}

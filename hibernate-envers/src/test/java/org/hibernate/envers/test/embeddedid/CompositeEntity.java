package org.hibernate.envers.test.embeddedid;

import java.util.Set;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.Table;

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

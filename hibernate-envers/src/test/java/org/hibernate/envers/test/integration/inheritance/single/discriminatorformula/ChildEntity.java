package org.hibernate.envers.test.integration.inheritance.single.discriminatorformula;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.hibernate.envers.Audited;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Entity
@DiscriminatorValue(ClassTypeEntity.CHILD_TYPE)
@Audited
public class ChildEntity extends ParentEntity {
	private String specificData;

	public ChildEntity() {
	}

	public ChildEntity(Long typeId, String data, String specificData) {
		super( typeId, data );
		this.specificData = specificData;
	}

	public ChildEntity(Long id, Long typeId, String data, String specificData) {
		super( id, typeId, data );
		this.specificData = specificData;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof ChildEntity) ) {
			return false;
		}
		if ( !super.equals( o ) ) {
			return false;
		}

		ChildEntity that = (ChildEntity) o;

		if ( specificData != null ? !specificData.equals( that.specificData ) : that.specificData != null ) {
			return false;
		}

		return true;
	}

	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + (specificData != null ? specificData.hashCode() : 0);
		return result;
	}

	public String toString() {
		return "ChildEntity(id = " + id + ", typeId = " + typeId + ", data = " + data + ", specificData = " + specificData + ")";
	}

	public String getSpecificData() {
		return specificData;
	}

	public void setSpecificData(String specificData) {
		this.specificData = specificData;
	}
}

package org.hibernate.envers.test.integration.inheritance.single.discriminatorformula;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.annotations.DiscriminatorFormula;
import org.hibernate.envers.Audited;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Entity
@DiscriminatorFormula(ParentEntity.DISCRIMINATOR_QUERY)
@DiscriminatorValue(ClassTypeEntity.PARENT_TYPE)
@Audited
public class ParentEntity {
	public static final String DISCRIMINATOR_QUERY = "(SELECT c.type FROM ClassTypeEntity c WHERE c.id = typeId)";

	@Id
	@GeneratedValue
	protected Long id;

	protected Long typeId;

	protected String data;

	public ParentEntity() {
	}

	public ParentEntity(Long typeId, String data) {
		this.typeId = typeId;
		this.data = data;
	}

	public ParentEntity(Long id, Long typeId, String data) {
		this.id = id;
		this.typeId = typeId;
		this.data = data;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof ParentEntity) ) {
			return false;
		}

		ParentEntity that = (ParentEntity) o;

		if ( id != null ? !id.equals( that.id ) : that.id != null ) {
			return false;
		}
		if ( typeId != null ? !typeId.equals( that.typeId ) : that.typeId != null ) {
			return false;
		}
		if ( data != null ? !data.equals( that.data ) : that.data != null ) {
			return false;
		}

		return true;
	}

	public int hashCode() {
		int result;
		result = (id != null ? id.hashCode() : 0);
		result = 31 * result + (typeId != null ? typeId.hashCode() : 0);
		result = 31 * result + (data != null ? data.hashCode() : 0);
		return result;
	}

	public String toString() {
		return "ParentEntity(id = " + id + ", typeId = " + typeId + ", data = " + data + ")";
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getTypeId() {
		return typeId;
	}

	public void setTypeId(Long typeId) {
		this.typeId = typeId;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}
}

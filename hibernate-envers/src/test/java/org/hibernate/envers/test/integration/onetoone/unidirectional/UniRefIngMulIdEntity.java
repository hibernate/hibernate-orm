package org.hibernate.envers.test.integration.onetoone.unidirectional;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;

import org.hibernate.envers.Audited;
import org.hibernate.envers.test.entities.ids.EmbIdTestEntity;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Entity
public class UniRefIngMulIdEntity {
	@Id
	private Integer id;

	@Audited
	private String data;

	@Audited
	@OneToOne
	private EmbIdTestEntity reference;

	public UniRefIngMulIdEntity() {
	}

	public UniRefIngMulIdEntity(Integer id, String data, EmbIdTestEntity reference) {
		this.id = id;
		this.data = data;
		this.reference = reference;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	public EmbIdTestEntity getReference() {
		return reference;
	}

	public void setReference(EmbIdTestEntity reference) {
		this.reference = reference;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof UniRefIngMulIdEntity) ) {
			return false;
		}

		UniRefIngMulIdEntity that = (UniRefIngMulIdEntity) o;

		if ( data != null ? !data.equals( that.data ) : that.data != null ) {
			return false;
		}
		if ( id != null ? !id.equals( that.id ) : that.id != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = (id != null ? id.hashCode() : 0);
		result = 31 * result + (data != null ? data.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "UniRefIngMulIdEntity[id = " + id + ", data = " + data + "]";
	}
}

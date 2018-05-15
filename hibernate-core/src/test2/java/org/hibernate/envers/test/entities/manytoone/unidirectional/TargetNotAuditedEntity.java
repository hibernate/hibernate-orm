/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.entities.manytoone.unidirectional;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;
import org.hibernate.envers.test.entities.UnversionedStrTestEntity;

/**
 * Audited entity with a reference to not audited entity.
 *
 * @author Toamsz Bech
 */
@Entity
public class TargetNotAuditedEntity {
	@Id
	private Integer id;

	@Audited
	private String data;

	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
	@ManyToOne(fetch = FetchType.LAZY)
	private UnversionedStrTestEntity reference;

	public TargetNotAuditedEntity() {
	}

	public TargetNotAuditedEntity(Integer id, String data, UnversionedStrTestEntity reference) {
		this.id = id;
		this.data = data;
		this.reference = reference;
	}

	public TargetNotAuditedEntity(String data, UnversionedStrTestEntity reference) {
		this.data = data;
		this.reference = reference;
	}

	public TargetNotAuditedEntity(Integer id, String data) {
		this.id = id;
		this.data = data;
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

	public UnversionedStrTestEntity getReference() {
		return reference;
	}

	public void setReference(UnversionedStrTestEntity reference) {
		this.reference = reference;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof TargetNotAuditedEntity) ) {
			return false;
		}

		TargetNotAuditedEntity that = (TargetNotAuditedEntity) o;

		if ( data != null ? !data.equals( that.getData() ) : that.getData() != null ) {
			return false;
		}
		if ( id != null ? !id.equals( that.getId() ) : that.getId() != null ) {
			return false;
		}

		return true;
	}

	public int hashCode() {
		int result;
		result = (id != null ? id.hashCode() : 0);
		result = 31 * result + (data != null ? data.hashCode() : 0);
		return result;
	}

	public String toString() {
		return "TargetNotAuditedEntity(id = " + id + ", data = " + data + ")";
	}
}

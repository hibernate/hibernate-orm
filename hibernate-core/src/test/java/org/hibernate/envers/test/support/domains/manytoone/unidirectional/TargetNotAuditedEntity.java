/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.support.domains.manytoone.unidirectional;

import java.util.Objects;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;
import org.hibernate.envers.test.support.domains.basic.UnversionedStrTestEntity;

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

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		TargetNotAuditedEntity that = (TargetNotAuditedEntity) o;
		return Objects.equals( id, that.id ) &&
				Objects.equals( data, that.data );
	}

	@Override
	public int hashCode() {
		return Objects.hash( id, data );
	}

	@Override
	public String toString() {
		return "TargetNotAuditedEntity{" +
				"id=" + id +
				", data='" + data + '\'' +
				'}';
	}
}

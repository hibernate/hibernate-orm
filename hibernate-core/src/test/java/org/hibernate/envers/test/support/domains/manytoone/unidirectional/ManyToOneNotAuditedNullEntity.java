/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.support.domains.manytoone.unidirectional;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;
import org.hibernate.envers.test.support.domains.basic.UnversionedStrTestEntity;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Audited
@Entity
@Table(name = "M2O_N_AUD_NULL")
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class ManyToOneNotAuditedNullEntity implements Serializable {
	@Id
	private Integer id;

	private String data;

	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
	@ManyToOne(fetch = FetchType.LAZY, optional = true)
	@NotFound(action = NotFoundAction.IGNORE)
	private UnversionedStrTestEntity reference;

	protected ManyToOneNotAuditedNullEntity() {
	}

	public ManyToOneNotAuditedNullEntity(Integer id, String data, UnversionedStrTestEntity reference) {
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
		ManyToOneNotAuditedNullEntity that = (ManyToOneNotAuditedNullEntity) o;
		return Objects.equals( id, that.id ) &&
				Objects.equals( data, that.data );
	}

	@Override
	public int hashCode() {
		return Objects.hash( id, data );
	}

	@Override
	public String toString() {
		return "ManyToOneNotAuditedNullEntity{" +
				"id=" + id +
				", data='" + data + '\'' +
				'}';
	}
}

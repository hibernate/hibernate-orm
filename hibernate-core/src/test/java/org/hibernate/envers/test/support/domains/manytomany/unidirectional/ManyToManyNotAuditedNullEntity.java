/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.support.domains.manytomany.unidirectional;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
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
@Table(name = "M2M_N_AUD_NULL")
public class ManyToManyNotAuditedNullEntity implements Serializable {
	@Id
	private Integer id;

	private String data;

	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
	@ManyToMany(fetch = FetchType.LAZY)
	@NotFound(action = NotFoundAction.IGNORE)
	@JoinTable(joinColumns = @JoinColumn(name = "M2MNotAudited_id"))
	private List<UnversionedStrTestEntity> references = new ArrayList<>();

	protected ManyToManyNotAuditedNullEntity() {
	}

	public ManyToManyNotAuditedNullEntity(Integer id, String data) {
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

	public List<UnversionedStrTestEntity> getReferences() {
		return references;
	}

	public void setReferences(List<UnversionedStrTestEntity> references) {
		this.references = references;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		ManyToManyNotAuditedNullEntity that = (ManyToManyNotAuditedNullEntity) o;
		return Objects.equals( id, that.id ) &&
				Objects.equals( data, that.data );
	}

	@Override
	public int hashCode() {
		return Objects.hash( id, data );
	}

	@Override
	public String toString() {
		return "ManyToManyNotAuditedNullEntity{" +
				"id=" + id +
				", data='" + data + '\'' +
				'}';
	}
}

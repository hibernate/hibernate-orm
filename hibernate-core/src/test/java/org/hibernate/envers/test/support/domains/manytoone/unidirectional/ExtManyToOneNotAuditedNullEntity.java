/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.support.domains.manytoone.unidirectional;

import java.util.Objects;

import javax.persistence.Entity;
import javax.persistence.Table;

import org.hibernate.envers.Audited;
import org.hibernate.envers.test.support.domains.basic.UnversionedStrTestEntity;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Entity
@Table(name = "EM2O_N_AUD_NULL")
public class ExtManyToOneNotAuditedNullEntity extends ManyToOneNotAuditedNullEntity {
	@Audited
	private String extension = null;

	public ExtManyToOneNotAuditedNullEntity() {
	}

	public ExtManyToOneNotAuditedNullEntity(Integer id, String data, UnversionedStrTestEntity reference, String extension) {
		super( id, data, reference );
		this.extension = extension;
	}

	public String getExtension() {
		return extension;
	}

	public void setExtension(String extension) {
		this.extension = extension;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		if ( !super.equals( o ) ) {
			return false;
		}
		ExtManyToOneNotAuditedNullEntity that = (ExtManyToOneNotAuditedNullEntity) o;
		return Objects.equals( extension, that.extension );
	}

	@Override
	public int hashCode() {
		return Objects.hash( super.hashCode(), extension );
	}

	@Override
	public String toString() {
		return "ExtManyToOneNotAuditedNullEntity{" +
				"extension='" + extension + '\'' +
				'}';
	}
}

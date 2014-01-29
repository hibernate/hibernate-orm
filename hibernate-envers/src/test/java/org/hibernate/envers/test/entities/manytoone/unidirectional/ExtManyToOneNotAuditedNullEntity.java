/*
* Hibernate, Relational Persistence for Idiomatic Java
*
* Copyright (c) 2014, Red Hat Inc. or third-party contributors as
* indicated by the @author tags or express copyright attribution
* statements applied by the authors. All third-party contributions are
* distributed under license by Red Hat Inc.
*
* This copyrighted material is made available to anyone wishing to use, modify,
* copy, or redistribute it subject to the terms and conditions of the GNU
* Lesser General Public License, as published by the Free Software Foundation.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
* or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
* for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this distribution; if not, write to:
* Free Software Foundation, Inc.
* 51 Franklin Street, Fifth Floor
* Boston, MA 02110-1301 USA
*/
package org.hibernate.envers.test.entities.manytoone.unidirectional;

import javax.persistence.Entity;
import javax.persistence.Table;

import org.hibernate.envers.Audited;
import org.hibernate.envers.test.entities.UnversionedStrTestEntity;

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

	@Override
	public boolean equals(Object o) {
		if ( this == o ) return true;
		if ( !( o instanceof ExtManyToOneNotAuditedNullEntity ) ) return false;
		if ( !super.equals( o ) ) return false;

		ExtManyToOneNotAuditedNullEntity that = (ExtManyToOneNotAuditedNullEntity) o;

		if ( extension != null ? !extension.equals( that.extension ) : that.extension != null ) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + (extension != null ? extension.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "ExtManyToOneNotAuditedNullEntity(" + super.toString() + ", extension = " + extension + ")";
	}

	public String getExtension() {
		return extension;
	}

	public void setExtension(String extension) {
		this.extension = extension;
	}
}

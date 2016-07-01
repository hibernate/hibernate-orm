/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.entities.ids;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.envers.Audited;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Entity
@Audited
public class UnusualIdNamingEntity implements Serializable {
	@Id
	private String uniqueField;

	private String variousData;

	public UnusualIdNamingEntity() {
	}

	public UnusualIdNamingEntity(String uniqueField, String variousData) {
		this.uniqueField = uniqueField;
		this.variousData = variousData;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof UnusualIdNamingEntity) ) {
			return false;
		}

		UnusualIdNamingEntity that = (UnusualIdNamingEntity) o;

		if ( uniqueField != null ? !uniqueField.equals( that.uniqueField ) : that.uniqueField != null ) {
			return false;
		}
		if ( variousData != null ? !variousData.equals( that.variousData ) : that.variousData != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = uniqueField != null ? uniqueField.hashCode() : 0;
		result = 31 * result + (variousData != null ? variousData.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "UnusualIdNamingEntity(uniqueField = " + uniqueField + ", variousData = " + variousData + ")";
	}

	public String getUniqueField() {
		return uniqueField;
	}

	public void setUniqueField(String uniqueField) {
		this.uniqueField = uniqueField;
	}

	public String getVariousData() {
		return variousData;
	}

	public void setVariousData(String variousData) {
		this.variousData = variousData;
	}
}

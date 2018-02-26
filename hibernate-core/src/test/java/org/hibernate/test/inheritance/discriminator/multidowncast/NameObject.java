/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.inheritance.discriminator.multidowncast;

import java.io.Serializable;
import javax.persistence.Embeddable;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

/**
 * @author Christian Beikov
 */
@Embeddable
public class NameObject implements Serializable {

	private String primaryName;
	private String secondaryName;
	private IntIdEntity intIdEntity;

	public NameObject() {
	}

	public NameObject(String primaryName, String secondaryName) {
		this.primaryName = primaryName;
		this.secondaryName = secondaryName;
	}

	public String getPrimaryName() {
		return primaryName;
	}

	public void setPrimaryName(String primaryName) {
		this.primaryName = primaryName;
	}

	public String getSecondaryName() {
		return secondaryName;
	}

	public void setSecondaryName(String secondaryName) {
		this.secondaryName = secondaryName;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "name_object_int_id_entity")
	public IntIdEntity getIntIdEntity() {
		return intIdEntity;
	}

	public void setIntIdEntity(IntIdEntity intIdEntity) {
		this.intIdEntity = intIdEntity;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !( o instanceof NameObject ) ) {
			return false;
		}

		NameObject that = (NameObject) o;

		if ( primaryName != null ? !primaryName.equals( that.primaryName ) : that.primaryName != null ) {
			return false;
		}
		return secondaryName != null ? secondaryName.equals( that.secondaryName ) : that.secondaryName == null;

	}

	@Override
	public int hashCode() {
		int result = primaryName != null ? primaryName.hashCode() : 0;
		result = 31 * result + ( secondaryName != null ? secondaryName.hashCode() : 0 );
		return result;
	}
}

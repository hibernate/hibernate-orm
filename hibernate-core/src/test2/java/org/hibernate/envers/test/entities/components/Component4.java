/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.entities.components;

import javax.persistence.Embeddable;

import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

/**
 * @author Kristoffer Lundberg (kristoffer at cambio dot se)
 */
@Embeddable
@Audited
public class Component4 {
	private String key;
	private String value;

	@NotAudited
	private String description;

	public Component4() {
	}

	public Component4(String key, String value, String description) {
		this.key = key;
		this.value = value;
		this.description = description;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((description == null) ? 0 : description.hashCode());
		result = prime * result + ((key == null) ? 0 : key.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !(obj instanceof Component4) ) {
			return false;
		}

		Component4 other = (Component4) obj;

		if ( description != null ? !description.equals( other.description ) : other.description != null ) {
			return false;
		}
		if ( key != null ? !key.equals( other.key ) : other.key != null ) {
			return false;
		}
		if ( value != null ? !value.equals( other.value ) : other.value != null ) {
			return false;
		}

		return true;
	}

	@Override
	public String toString() {
		return "Component4[key = " + key + ", value = " + value + ", description = " + description + "]";
	}
}

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.support.domains.components;

import java.util.Objects;

import javax.persistence.Embeddable;

import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

/**
 * @author Kristoffer Lundberg (kristoffer at cambio dot se)
 * @author Chris Cranford
 */
@Embeddable
@Audited
public class PartialAuditedNestedComponent {
	private String key;
	private String value;

	@NotAudited
	private String description;

	public PartialAuditedNestedComponent() {
	}

	public PartialAuditedNestedComponent(String key, String value, String description) {
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
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		PartialAuditedNestedComponent that = (PartialAuditedNestedComponent) o;
		return Objects.equals( key, that.key ) &&
				Objects.equals( value, that.value ) &&
				Objects.equals( description, that.description );
	}

	@Override
	public int hashCode() {
		return Objects.hash( key, value, description );
	}

	@Override
	public String toString() {
		return "PartialAuditedNestedComponent{" +
				"key='" + key + '\'' +
				", value='" + value + '\'' +
				", description='" + description + '\'' +
				'}';
	}
}

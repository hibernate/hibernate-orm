/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.support.domains.components.dynamic;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

/**
 * @author Chris Cranford
 */
@Audited
public class DynamicMapComponentNotAuditedEntity {
	private long id;
	private String note;
	private Map<String, Object> customFields = new HashMap<>();
	private Map<String, Object> customFields2 = new HashMap<>();

	public DynamicMapComponentNotAuditedEntity() {

	}

	public DynamicMapComponentNotAuditedEntity(long id, String note) {
		this.id = id;
		this.note = note;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getNote() {
		return note;
	}

	public void setNote(String note) {
		this.note = note;
	}

	// Dynamic components are not supported by Envers, so @NotAudited
	@NotAudited
	public Map<String, Object> getCustomFields() {
		return customFields;
	}

	public void setCustomFields(Map<String, Object> customFields) {
		this.customFields = customFields;
	}

	@NotAudited
	public Map<String, Object> getCustomFields2() {
		return customFields2;
	}

	public void setCustomFields2(Map<String, Object> customFields2) {
		this.customFields2 = customFields2;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		DynamicMapComponentNotAuditedEntity that = (DynamicMapComponentNotAuditedEntity) o;
		return id == that.id &&
				Objects.equals( note, that.note ) &&
				Objects.equals( customFields, that.customFields ) &&
				Objects.equals( customFields2, that.customFields2 );
	}

	@Override
	public int hashCode() {
		return Objects.hash( id, note, customFields, customFields2 );
	}

	@Override
	public String toString() {
		return "DynamicMapComponentNotAuditedEntity{" +
				"id=" + id +
				", note='" + note + '\'' +
				", customFields=" + customFields +
				'}';
	}
}

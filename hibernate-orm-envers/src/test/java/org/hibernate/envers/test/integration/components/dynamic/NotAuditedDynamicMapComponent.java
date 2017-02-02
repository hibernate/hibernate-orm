/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.components.dynamic;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Audited
public class NotAuditedDynamicMapComponent implements Serializable {
	private long id;
	private String note;
	private Map<String, Object> customFields = new HashMap<String, Object>();

	public NotAuditedDynamicMapComponent() {
	}

	public NotAuditedDynamicMapComponent(long id, String note) {
		this.id = id;
		this.note = note;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !( o instanceof NotAuditedDynamicMapComponent ) ) {
			return false;
		}

		NotAuditedDynamicMapComponent that = (NotAuditedDynamicMapComponent) o;

		if ( id != that.id ) {
			return false;
		}
		if ( customFields != null ? !customFields.equals( that.customFields ) : that.customFields != null ) {
			return false;
		}
		if ( note != null ? !note.equals( that.note ) : that.note != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = (int) ( id ^ ( id >>> 32 ) );
		result = 31 * result + ( note != null ? note.hashCode() : 0 );
		result = 31 * result + ( customFields != null ? customFields.hashCode() : 0 );
		return result;
	}

	@Override
	public String toString() {
		return "NotAuditedDynamicMapComponent(id = " + id + ", note = " + note + ", customFields = " + customFields + ")";
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

	@NotAudited // Dynamic components are not supported for audition.
	public Map<String, Object> getCustomFields() {
		return customFields;
	}

	public void setCustomFields(Map<String, Object> customFields) {
		this.customFields = customFields;
	}
}

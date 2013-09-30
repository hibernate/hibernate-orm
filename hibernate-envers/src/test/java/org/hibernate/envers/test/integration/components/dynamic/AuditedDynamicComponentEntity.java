package org.hibernate.envers.test.integration.components.dynamic;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.envers.Audited;

@Audited
public class AuditedDynamicComponentEntity implements Serializable {
	private long id;
	private String note;
	private Map<String, Object> customFields = new HashMap<String, Object>();
	private SimpleEntity simpleEntity;

	public AuditedDynamicComponentEntity() {
	}

	public AuditedDynamicComponentEntity(long id, String note) {
		this.id = id;
		this.note = note;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !( o instanceof AuditedDynamicComponentEntity ) ) {
			return false;
		}

		AuditedDynamicComponentEntity that = (AuditedDynamicComponentEntity) o;

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
		return "AuditedDynamicMapComponent(id = " + id + ", note = " + note + ", customFields = " + customFields + ")";
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

	public Map<String, Object> getCustomFields() {
		return customFields;
	}

	public void setCustomFields(Map<String, Object> customFields) {
		this.customFields = customFields;
	}


	public SimpleEntity getSimpleEntity() {
		return simpleEntity;
	}

	public void setSimpleEntity(SimpleEntity simpleEntity) {
		this.simpleEntity = simpleEntity;
	}
}
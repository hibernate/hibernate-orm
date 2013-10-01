package org.hibernate.envers.test.integration.components.dynamic;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.envers.Audited;

@Audited
public class AdvancedEntity {

	private Long id;

	private String note;

	private Map<String, Object> dynamicConfiguration = new HashMap<String, Object>();

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getNote() {
		return note;
	}

	public void setNote(String note) {
		this.note = note;
	}

	public Map<String, Object> getDynamicConfiguration() {
		return dynamicConfiguration;
	}

	public void setDynamicConfiguration(Map<String, Object> dynamicConfiguration) {
		this.dynamicConfiguration = dynamicConfiguration;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !( o instanceof AdvancedEntity ) ) {
			return false;
		}

		AdvancedEntity that = (AdvancedEntity) o;

		if ( dynamicConfiguration != null ? !dynamicConfiguration.equals( that.dynamicConfiguration ) : that.dynamicConfiguration != null ) {
			return false;
		}
		if ( id != null ? !id.equals( that.id ) : that.id != null ) {
			return false;
		}
		if ( note != null ? !note.equals( that.note ) : that.note != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = id != null ? id.hashCode() : 0;
		result = 31 * result + ( note != null ? note.hashCode() : 0 );
		result = 31 * result + ( dynamicConfiguration != null ? dynamicConfiguration.hashCode() : 0 );
		return result;
	}

	@Override
	public String toString() {
		return "AdvancedEntity{" +
				"id=" + id +
				", note='" + note + '\'' +
				", dynamicConfiguration=" + dynamicConfiguration +
				'}';
	}


}

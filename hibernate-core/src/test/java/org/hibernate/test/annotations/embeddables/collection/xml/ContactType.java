package org.hibernate.test.annotations.embeddables.collection.xml;

import java.io.Serializable;

/**
 * <code>ContactType</code> -
 *
 * @author Vlad Mihalcea
 */
public class ContactType implements Serializable {

	private Long id;

	private int version;

	private String type;

	public Long getId() {
		return this.id;
	}

	public void setId(final Long id) {
		this.id = id;
	}

	public int getVersion() {
		return this.version;
	}

	public void setVersion(final int version) {
		this.version = version;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !( obj instanceof ContactType ) ) {
			return false;
		}
		ContactType other = (ContactType) obj;
		if ( id != null ) {
			if ( !id.equals( other.id ) ) {
				return false;
			}
		}
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ( ( id == null ) ? 0 : id.hashCode() );
		return result;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	@Override
	public String toString() {
		String result = getClass().getSimpleName() + " ";
		if ( id != null ) {
			result += "id: " + id;
		}
		result += ", version: " + version;
		if ( type != null && !type.trim().isEmpty() ) {
			result += ", type: " + type;
		}
		return result;
	}
}

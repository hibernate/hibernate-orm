package org.hibernate.envers.test.integration.ids.protectedmodifier;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import java.io.Serializable;

import org.hibernate.envers.Audited;

@Entity
@Audited
public class ProtectedConstructorEntity implements Serializable {
	@EmbeddedId
	private WrappedStringId wrappedStringId;

	private String str1;

	@SuppressWarnings("unused")
	protected ProtectedConstructorEntity() {
		// For JPA. Protected access modifier is essential in terms of unit test.
	}

	public ProtectedConstructorEntity(WrappedStringId wrappedStringId, String str1) {
		this.wrappedStringId = wrappedStringId;
		this.str1 = str1;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof ProtectedConstructorEntity) ) {
			return false;
		}

		ProtectedConstructorEntity that = (ProtectedConstructorEntity) o;

		if ( wrappedStringId != null ?
				!wrappedStringId.equals( that.wrappedStringId ) :
				that.wrappedStringId != null ) {
			return false;
		}
		if ( str1 != null ? !str1.equals( that.str1 ) : that.str1 != null ) {
			return false;
		}

		return true;
	}

	public int hashCode() {
		int result = (wrappedStringId != null ? wrappedStringId.hashCode() : 0);
		result = 31 * result + (str1 != null ? str1.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "ProtectedConstructorEntity(wrappedStringId = " + wrappedStringId + ", str1 = " + str1 + ")";
	}

	public WrappedStringId getWrappedStringId() {
		return wrappedStringId;
	}

	public void setWrappedStringId(WrappedStringId wrappedStringId) {
		this.wrappedStringId = wrappedStringId;
	}

	public String getStr1() {
		return str1;
	}

	public void setStr1(String str1) {
		this.str1 = str1;
	}
}

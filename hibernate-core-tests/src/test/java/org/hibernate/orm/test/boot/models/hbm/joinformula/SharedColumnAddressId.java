package org.hibernate.orm.test.boot.models.hbm.joinformula;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author Steve Ebersole
 */
public class SharedColumnAddressId implements Serializable {
	private String person;
	private String type;

	public SharedColumnAddressId() {
	}

	public SharedColumnAddressId(String person, String type) {
		this.person = person;
		this.type = type;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof SharedColumnAddressId that) ) {
			return false;
		}
		return Objects.equals( person, that.person ) && Objects.equals( type, that.type );
	}

	@Override
	public int hashCode() {
		return Objects.hash( person, type );
	}
}

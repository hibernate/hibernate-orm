//$Id$
package org.hibernate.test.annotations.reflection;
import java.io.Serializable;
import javax.persistence.Embeddable;

/**
 * @author Emmanuel Bernard
 */
@Embeddable
public class SocialSecurityNumber implements Serializable {
	public String number;
	public String countryCode;

	public boolean equals(Object o) {
		if ( this == o ) return true;
		if ( o == null || getClass() != o.getClass() ) return false;

		final SocialSecurityNumber that = (SocialSecurityNumber) o;

		if ( !countryCode.equals( that.countryCode ) ) return false;
		if ( !number.equals( that.number ) ) return false;

		return true;
	}

	public int hashCode() {
		int result;
		result = number.hashCode();
		result = 29 * result + countryCode.hashCode();
		return result;
	}
}

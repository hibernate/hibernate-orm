//$Id$
package org.hibernate.test.annotations.join;
import java.io.Serializable;
import javax.persistence.Embeddable;

/**
 * @author Emmanuel Bernard
 */
@Embeddable
public class DogPk implements Serializable {
	public String name;
	public String ownerName;

	public boolean equals(Object o) {
		if ( this == o ) return true;
		if ( !( o instanceof DogPk ) ) return false;

		final DogPk dogPk = (DogPk) o;

		if ( !name.equals( dogPk.name ) ) return false;
		if ( !ownerName.equals( dogPk.ownerName ) ) return false;

		return true;
	}

	public int hashCode() {
		int result;
		result = name.hashCode();
		result = 29 * result + ownerName.hashCode();
		return result;
	}
}

package org.hibernate.orm.test.onetoone.formula.conversion;

import java.io.Serializable;
import java.util.Objects;

/// Identifier used by the converted address mappings.
///
/// @author Steve Ebersole
public class AddressId implements Serializable {
	public String person;
	public String type;

	public AddressId() {}

	public AddressId(String person, String type) {
		this.person = person;
		this.type = type;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof AddressId id
				&& Objects.equals(person, id.person) && Objects.equals(type, id.type);
	}

	@Override
	public int hashCode() {
		return Objects.hash(person, type);
	}
}

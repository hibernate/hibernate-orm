package org.hibernate.processor.test.jpa4;

import java.io.Serializable;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
class RawComparableEntity {
	@Id
	Long id;

	RawValue direct;

	InheritedRawValue inherited;

	static class RawValue implements Comparable, Serializable {
		@Override
		public int compareTo(Object other) {
			return 0;
		}
	}

	static class InheritedRawValue extends RawValue {
	}
}

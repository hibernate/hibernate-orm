package org.hibernate.processor.test.jpa4;

import java.io.Serializable;
import java.time.temporal.Temporal;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
class RawComparableEntity {
	@Id
	Long id;

	RawValue direct;

	InheritedRawValue inherited;

	RawNumber number;

	RawTemporal temporal;

	static class RawValue implements Comparable, Serializable {
		@Override
		public int compareTo(Object other) {
			return 0;
		}
	}

	static class InheritedRawValue extends RawValue {
	}

	abstract static class RawNumber extends Number implements Comparable {
	}

	abstract static class RawTemporal implements Temporal, Comparable, Serializable {
	}
}

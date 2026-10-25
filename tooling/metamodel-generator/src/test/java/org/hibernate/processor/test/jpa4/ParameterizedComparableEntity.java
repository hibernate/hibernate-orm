package org.hibernate.processor.test.jpa4;

import java.io.Serializable;
import java.time.temporal.Temporal;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
class ParameterizedComparableEntity {
	@Id
	Long id;

	Value<String> value;

	Child inherited;

	NumericValue<String> number;

	TemporalValue<String> temporal;

	static class Value<T> implements Comparable<Value<T>>, Serializable {
		@Override
		public int compareTo(Value<T> other) {
			return 0;
		}
	}

	static class Base<T> implements Comparable<Base<T>>, Serializable {
		@Override
		public int compareTo(Base<T> other) {
			return 0;
		}
	}

	static class Child extends Base<String> {
	}

	abstract static class NumericValue<T> extends Number implements Comparable<NumericValue<T>> {
	}

	abstract static class TemporalValue<T> implements Temporal, Comparable<TemporalValue<T>>, Serializable {
	}
}

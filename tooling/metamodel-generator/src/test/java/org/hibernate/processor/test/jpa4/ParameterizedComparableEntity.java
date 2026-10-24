package org.hibernate.processor.test.jpa4;

import java.io.Serializable;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
class ParameterizedComparableEntity {
	@Id
	Long id;

	Value<String> value;

	Child inherited;

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
}

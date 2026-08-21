/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping;

import java.io.Serializable;
import java.util.Objects;

/**
 * Represents the discriminator value used for entity mappings.
 */
public sealed interface DiscriminatorValue extends Serializable {

	Object value();

	static DiscriminatorValue of(Object value) {
		return value == null ? Special.NULL : new Literal( value );
	}

	record Literal(Object value) implements DiscriminatorValue {
		public Literal {
			Objects.requireNonNull( value, "discriminator literal value must not be null" );
		}

		@Override
		public String toString() {
			return value.toString();
		}

		@Override
		public boolean equals(Object object) {
			return this == object
				|| object instanceof Literal that && this.value.equals(that.value);
		}

		@Override
		public int hashCode() {
			return value.hashCode();
		}
	}

	enum Special implements DiscriminatorValue {
		NULL,
		NOT_NULL;

		@Override
		public Object value() {
			return switch ( this ) {
				case NULL -> null;
				case NOT_NULL ->
						throw new IllegalStateException( "Cannot obtain a discriminator value for NOT_NULL mapping" );
			};
		}

		@Override
		public String toString() {
			return switch ( this ) {
				case NULL -> "<null discriminator>";
				case NOT_NULL -> "<not null discriminator>";
			};
		}
	}
}

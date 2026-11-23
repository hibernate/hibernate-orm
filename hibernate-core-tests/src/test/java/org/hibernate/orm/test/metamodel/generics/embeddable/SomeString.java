package org.hibernate.orm.test.metamodel.generics.embeddable;

import jakarta.persistence.Embeddable;

@Embeddable
public class SomeString extends AbstractValueObject<String> {
	protected SomeString() {
	}

	public SomeString(final String value) {
		super( value );
	}
}

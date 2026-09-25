package org.hibernate.orm.test.metamodel.generics.embeddable;

import java.util.Date;

import jakarta.persistence.Embeddable;

@Embeddable
public class CreationDate extends AbstractValueObject<Date> {
	protected CreationDate() {
		super();
	}

	public CreationDate(final Date value) {
		super( value );
	}
}

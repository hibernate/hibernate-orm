package org.hibernate.generator.internal;

import org.hibernate.HibernateException;

public class CompositeValueGenerationException extends HibernateException {
	public CompositeValueGenerationException(String message) {
		super(message);
	}
}

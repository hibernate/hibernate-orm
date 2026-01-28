/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.util;

import org.hibernate.mapping.Property;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Helper iterator to ignore "backrefs" properties in hibernate mapping model.
 *
 * @author Max Rydahl Andersen
 *
 */
public class SkipBackRefPropertyIterator implements Iterator<Property> {

	private final Iterator<?> delegate;

	private Property backLog;

	public SkipBackRefPropertyIterator(Iterator<?> iterator) {
		delegate = iterator;
	}

	public boolean hasNext() {
		if ( backLog!=null ) {
			return true;
		}
		else if ( delegate.hasNext() ) {
			Property nextProperty = (Property) delegate.next();
			while ( nextProperty.isBackRef() && delegate.hasNext() ) {
				nextProperty = (Property) delegate.next();
			}
			if ( !nextProperty.isBackRef() ) {
				backLog = nextProperty;
				return true;
			}
		}
		return false;
	}

	public Property next() {
		if ( backLog != null ) {
			Property p = backLog;
			backLog = null;
			return p;
		}
		Property nextProperty = (Property) delegate.next();
		while ( nextProperty.isBackRef() && delegate.hasNext() ) {
			nextProperty = (Property) delegate.next();
		}
		if ( nextProperty.isBackRef() ) {
			throw new NoSuchElementException();
		}
		return nextProperty;
	}

	public void remove() {
		throw new UnsupportedOperationException( "remove() not allowed" );
	}

}

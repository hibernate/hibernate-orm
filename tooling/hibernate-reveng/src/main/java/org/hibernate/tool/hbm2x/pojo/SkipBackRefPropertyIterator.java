package org.hibernate.tool.hbm2x.pojo;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.hibernate.mapping.Property;

/**
 * Helper iterator to ignore "backrefs" properties in hibernate mapping model.
 *
 * @author Max Rydahl Andersen
 *
 */
public class SkipBackRefPropertyIterator implements Iterator<Property> {

	private Iterator<?> delegate;

	private Property backLog;

	public SkipBackRefPropertyIterator(Iterator<?> iterator) {
		delegate = iterator;
	}

	public boolean hasNext() {
		if ( backLog!=null ) {
			return true;
		} else if ( delegate.hasNext() ) {
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

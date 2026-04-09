/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2010-2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.internal.util;

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

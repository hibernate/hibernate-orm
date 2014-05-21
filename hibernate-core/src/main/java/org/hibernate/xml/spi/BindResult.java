/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.xml.spi;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

import javax.xml.transform.Source;

import org.hibernate.internal.CoreLogging;
import org.hibernate.metamodel.source.spi.InvalidMappingException;
import org.hibernate.xml.internal.jaxb.AbstractUnifiedBinder;
import org.jboss.logging.Logger;

/**
 * Return object for the result of performing JAXB binding.
 *
 * @author Hardy Ferentschik
 * @author Steve Ebersole
 */
public class BindResult<T> implements Serializable {
	private static final Logger LOG = CoreLogging.logger( BindResult.class );
	
	private final DelayedBinder<T> binder;
	
	private final Origin origin;
	
	private final boolean close;
	
	private T root;

	public BindResult(Source source, Origin origin) {
		binder = new DelayedSourceBinder<T>( source );
		this.origin = origin;
		close = false;
	}

	public BindResult(InputStream inputStream, Origin origin, boolean close) {
		binder = new DelayedInputStreamBinder<T>( inputStream );
		this.origin = origin;
		this.close = close;
	}
	
	public void bind(AbstractUnifiedBinder<T> jaxbProcessor) {
		root = binder.bind( jaxbProcessor );
	}

	/**
	 * Obtain the root JAXB bound object
	 *
	 * @return The JAXB root object
	 */
	public T getRoot() {
		return root;
	}

	/**
	 * Obtain the metadata about the document's origin
	 *
	 * @return The origin
	 */
	public Origin getOrigin() {
		return origin;
	}
	
	private interface DelayedBinder<T> {
		public T bind(AbstractUnifiedBinder<T> jaxbProcessor);
	}
	
	private class DelayedSourceBinder<T> implements DelayedBinder<T> {
		private final Source source;
		
		public DelayedSourceBinder(Source source) {
			this.source = source;
		}
		
		public T bind(AbstractUnifiedBinder<T> jaxbProcessor) {
			return jaxbProcessor.bind( source, origin );
		}
	}
	
	private class DelayedInputStreamBinder<T> implements DelayedBinder<T> {
		private final InputStream inputStream;
		
		public DelayedInputStreamBinder(InputStream inputStream) {
			this.inputStream = inputStream;
		}
		
		public T bind(AbstractUnifiedBinder<T> jaxbProcessor) {
			try {
				return jaxbProcessor.bind( inputStream, origin );
			}
			catch ( Exception e ) {
				throw new InvalidMappingException( origin, e );
			}
			finally {
				if ( close ) {
					try {
						inputStream.close();
					}
					catch ( IOException ignore ) {
						LOG.trace( "Was unable to close input stream" );
					}
				}
			}
		}
	}
}

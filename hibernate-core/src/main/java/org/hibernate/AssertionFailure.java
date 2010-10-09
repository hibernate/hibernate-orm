/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Indicates failure of an assertion: a possible bug in Hibernate.
 *
 * @author Gavin King
 */
public class AssertionFailure extends RuntimeException {

	private static final Logger log = LoggerFactory.getLogger( AssertionFailure.class );

	private static final String MESSAGE = "an assertion failure occured" +
			" (this may indicate a bug in Hibernate, but is more likely due" +
			" to unsafe use of the session)";

	public AssertionFailure(String s) {
		super( s );
		log.error( MESSAGE, this );
	}

	public AssertionFailure(String s, Throwable t) {
		super( s, t );
		log.error( MESSAGE, t );
	}

}

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.loader.plan.spi;

import org.hibernate.HibernateException;

/**
 * Indicates an attempt to lookup a QuerySpace by its uid, when no registration has been made under that uid.
 *
 * @author Steve Ebersole
 */
public class QuerySpaceUidNotRegisteredException extends HibernateException {
	public QuerySpaceUidNotRegisteredException(String uid) {
		super( generateMessage( uid ) );
	}

	private static String generateMessage(String uid) {
		return "Given uid [" + uid + "] could not be resolved to a QuerySpace";
	}

	public QuerySpaceUidNotRegisteredException(String uid, Throwable cause) {
		super( generateMessage( uid ), cause );
	}
}

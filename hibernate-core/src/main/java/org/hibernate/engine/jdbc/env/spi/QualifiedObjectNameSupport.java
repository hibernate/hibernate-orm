/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.engine.jdbc.env.spi;

import org.hibernate.metamodel.spi.relational.ObjectName;

/**
 * Defines support for reading and writing qualified object names to and from the database.  Generally speaking
 * Hibernate itself only uses {@link #formatName}.  Most times when it is "parsing" object names it is coming from
 * mappings, in which case we expect simple dot-separated syntax and apply {@link ObjectName#parse}
 *
 * @author Steve Ebersole
 */
public interface QualifiedObjectNameSupport {
	/**
	 * Performs formatting of an ObjectName to its String representation
	 *
	 * @param objectName The object name to be formatted.
	 *
	 * @return The dialect specific string form of the name.
	 */
	public String formatName(ObjectName objectName);

	/**
	 * Parse a String representation of an Object name to its ObjectName.  Note that this specifically
	 * attempts to parse the text as if coming from the database.  Mapping forms always use
	 * the form {@code <schema>.<catalog>.<name>}, parsing such names should just use {@link ObjectName#parse}
	 *
	 * @param text The object name text
	 *
	 * @return The parsed ObjectName
	 */
	public ObjectName parseName(String text);
}

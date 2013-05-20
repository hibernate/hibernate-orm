/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.id;

import java.io.Serializable;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;

/**
 * The general contract between a class that generates unique
 * identifiers and the <tt>Session</tt>. It is not intended that
 * this interface ever be exposed to the application. It <b>is</b>
 * intended that users implement this interface to provide
 * custom identifier generation strategies.<br>
 * <br>
 * Implementors should provide a public default constructor.<br>
 * <br>
 * Implementations that accept configuration parameters should
 * also implement <tt>Configurable</tt>.
 * <br>
 * Implementors <em>must</em> be thread-safe
 *
 * @author Gavin King
 *
 * @see PersistentIdentifierGenerator
 * @see Configurable
 */
public interface IdentifierGenerator {
	/**
	 * The configuration parameter holding the entity name
	 */
	public static final String ENTITY_NAME = "entity_name";

	/**
	 * The configuration parameter holding the JPA entity name
	 */
	public static final String JPA_ENTITY_NAME = "jpa_entity_name";

	/**
	 * Generate a new identifier.
	 *
	 * @param session The session from which the request originates
	 * @param object the entity or collection (idbag) for which the id is being generated
	 *
	 * @return a new identifier
	 *
	 * @throws HibernateException Indicates trouble generating the identifier
	 */
	public Serializable generate(SessionImplementor session, Object object) throws HibernateException;
}

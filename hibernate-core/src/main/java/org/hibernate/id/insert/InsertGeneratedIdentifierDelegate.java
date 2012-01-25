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
package org.hibernate.id.insert;
import java.io.Serializable;

import org.hibernate.engine.spi.SessionImplementor;

/**
 * Responsible for handling delegation relating to variants in how
 * insert-generated-identifier generator strategies dictate processing:<ul>
 * <li>building the sql insert statement
 * <li>determination of the generated identifier value
 * </ul>
 *
 * @author Steve Ebersole
 */
public interface InsertGeneratedIdentifierDelegate {

	/**
	 * Build a {@link org.hibernate.sql.Insert} specific to the delegate's mode
	 * of handling generated key values.
	 *
	 * @return The insert object.
	 */
	public IdentifierGeneratingInsert prepareIdentifierGeneratingInsert();

	/**
	 * Perform the indicated insert SQL statement and determine the identifier value
	 * generated.
	 *
	 *
	 * @param insertSQL The INSERT statement string
	 * @param session The session in which we are operating
	 * @param binder The param binder
	 * 
	 * @return The generated identifier value.
	 */
	public Serializable performInsert(String insertSQL, SessionImplementor session, Binder binder);

}

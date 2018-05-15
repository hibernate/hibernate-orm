/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.ast.tree.spi.expression.instantiation;

import org.hibernate.HibernateException;

/**
 * Indicates a problem performing a dynamic instantiation
 *
 * @author Steve Ebersole
 */
public class InstantiationException extends HibernateException {
	public InstantiationException(String message) {
		super( message );
	}

	public InstantiationException(String message, Throwable cause) {
		super( message, cause );
	}
}

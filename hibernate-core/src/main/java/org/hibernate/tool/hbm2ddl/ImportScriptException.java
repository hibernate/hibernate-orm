/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.hbm2ddl;

import org.hibernate.HibernateException;

/**
 * @author Steve Ebersole
 */
public class ImportScriptException extends HibernateException {
	public ImportScriptException(String s) {
		super( s );
	}

	public ImportScriptException(String string, Throwable root) {
		super( string, root );
	}
}

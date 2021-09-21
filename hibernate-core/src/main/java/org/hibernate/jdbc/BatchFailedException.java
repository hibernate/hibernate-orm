/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jdbc;
import org.hibernate.HibernateException;

/**
 * Indicates a failed batch entry (-3 return).
 *
 * @author Steve Ebersole
 */
public class BatchFailedException extends HibernateException {
	public BatchFailedException(String s) {
		super( s );
	}

	public BatchFailedException(String string, Throwable root) {
		super( string, root );
	}
}

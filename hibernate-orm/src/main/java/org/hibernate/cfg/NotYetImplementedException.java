/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg;
import org.hibernate.MappingException;

/**
 * Mapping not yet implemented
 *
 * @author Emmanuel Bernard
 */
public class NotYetImplementedException extends MappingException {
	public NotYetImplementedException() {
		this( "Not yet implemented!" );
	}

	public NotYetImplementedException(String msg, Throwable root) {
		super( msg, root );
	}

	public NotYetImplementedException(Throwable root) {
		super( root );
	}

	public NotYetImplementedException(String s) {
		super( s );
	}

}

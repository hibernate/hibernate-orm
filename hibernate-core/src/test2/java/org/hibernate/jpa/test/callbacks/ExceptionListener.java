/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.jpa.test.callbacks;
import javax.persistence.PrePersist;

/**
 * @author Emmanuel Bernard
 */
public class ExceptionListener {
	@PrePersist
	public void raiseException(Object e) {
		throw new ArithmeticException( "1/0 impossible" );
	}
}

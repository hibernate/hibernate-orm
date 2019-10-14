/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.session.runtime.check;

public class CheckForbiddenAPIException extends RuntimeException {

	public CheckForbiddenAPIException(String forbiddenAPI) {
		super( forbiddenAPI + " is not supposed to be used at runtime" );
	}
}

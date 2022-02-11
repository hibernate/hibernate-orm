/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;

import org.hibernate.FetchNotFoundException;

/**
 * Possible actions when the database contains a non-null fk with no
 * matching target. This also implies that there are no physical
 * foreign-key constraints on the database.
 *
 * As an example, consider a typical Customer/Order model.  These actions apply
 * when a non-null `orders.customer_fk` value does not have a corresponding value
 * in `customers.id`.
 *
 * Generally this will occur in 2 scenarios:<ul>
 *     <li>the associated data has been deleted</li>
 *     <li>the model uses special "magic" values to indicate null</li>
 * </ul>
 *
 * @author Steve Ebersole
 * @author Emmanuel Bernard
 */
public enum NotFoundAction {
	/**
	 * Throw an exception when the association is not found (default and recommended).
	 *
	 * @see FetchNotFoundException
	 */
	EXCEPTION,

	/**
	 * Ignore the association when not found in database.  Effectively treats the
	 * association as null, despite the non-null foreign-key value.
	 */
	IGNORE
}

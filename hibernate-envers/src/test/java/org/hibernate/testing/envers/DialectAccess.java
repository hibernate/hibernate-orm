/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.envers;

import org.hibernate.dialect.Dialect;


/**
 * Contract for things that expose a Dialect
 *
 * @author Steve Ebersole
 */
public interface DialectAccess {
	Dialect getDialect();
}

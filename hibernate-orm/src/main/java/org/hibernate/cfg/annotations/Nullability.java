/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg.annotations;


/**
 * Are the columns forced to null, not null or not forced
 *
 * @author Emmanuel Bernard
 */
public enum Nullability {
	FORCED_NULL,
	FORCED_NOT_NULL,
	NO_CONSTRAINT
}

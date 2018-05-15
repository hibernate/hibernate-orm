/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

/**
 * Package defining the Hibernate run-time view of the application domain model
 *
 * todo (6.0) : clean up this package.  Most is superseded by the Navigable hierarchy.
 *		Currently Hibernate implements the JPA metamodel contracts with a series
 *		of wrapper objects defined in the `org.hibernate.metamodel` package.  This
 *		is non-ideal because it is a wrapper approach rather than integrating the
 *		JPA contracts into our runtime model which we are doing in these 6.0
 *		changes (Navigable ties in these JPA contracts).
 */
@Incubating
package org.hibernate.metamodel;

import org.hibernate.Incubating;

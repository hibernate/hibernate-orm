/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: package-info.java 14736 2008-06-04 14:23:42Z hardy.ferentschik $
/**
 * Test package for metatata facilities
 * It contains an example of package level metadata
 */
@org.hibernate.annotations.GenericGenerator(name = "system-uuid", strategy = "uuid")
@org.hibernate.annotations.GenericGenerators(
		@org.hibernate.annotations.GenericGenerator(name = "system-uuid-2", strategy = "uuid")
)
package org.hibernate.test.annotations.id.sequences;



/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.processor.test.separatecompilationunits;

import org.hibernate.processor.test.separatecompilationunits.superclass.MappedSuperclass;

/**
 * @author Hardy Ferentschik
 */
@jakarta.persistence.Entity
public class Entity extends MappedSuperclass {
	private String name;
}



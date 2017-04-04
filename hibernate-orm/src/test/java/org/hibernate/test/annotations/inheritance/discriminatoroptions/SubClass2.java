/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.inheritance.discriminatoroptions;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * @author Hardy Ferentschik
 */
@Entity
@DiscriminatorValue("B")
public class SubClass2 extends BaseClass2 {
}



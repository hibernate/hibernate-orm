/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.inheritance.discriminator.joinedsubclass;

import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

/**
 * @author Andrea Boriero
 */
@jakarta.persistence.Entity
@Inheritance(strategy = InheritanceType.JOINED)
public interface TestEntityInterface extends Common {
}

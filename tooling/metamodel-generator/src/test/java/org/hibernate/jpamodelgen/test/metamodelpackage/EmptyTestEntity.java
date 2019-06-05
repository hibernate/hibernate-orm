/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.test.metamodelpackage;

import javax.persistence.Entity;

/**
 * This class models, for example, and type that differs only the the parent of a one-to-many relationship.
 *
 * @author Marvin S. Addison
 */
@Entity
public class EmptyTestEntity extends TestEntity {
}

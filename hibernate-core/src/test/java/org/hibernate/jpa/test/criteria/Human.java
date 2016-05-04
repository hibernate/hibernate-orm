/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.criteria;

import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * Human
 */
@Entity
@Table(name = "HUMAN")
public class Human extends Animal {
}
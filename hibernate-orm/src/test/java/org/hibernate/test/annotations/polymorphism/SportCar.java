/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.test.annotations.polymorphism;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.hibernate.annotations.PolymorphismType;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Table(name = "sport_car")
@org.hibernate.annotations.Entity(polymorphism = PolymorphismType.EXPLICIT) //raise a warn
public class SportCar extends Car {
}

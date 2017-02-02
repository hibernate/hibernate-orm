/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.entity;
import javax.persistence.Entity;

import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.TypeDef;

@TypeDef(
		name = "definedInDerivedClass",
		typeClass = CasterStringType.class,
		parameters = {
			@Parameter(name = "cast", value = "upper")
		}
)

/**
 * Defines a custom type that is used in the 
 * base class. 
 * @author Sharath Reddy
 *
 */
@Entity
public class Peugot extends Car {

}

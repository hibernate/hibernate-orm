/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.inheritance.discriminatoroptions;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.annotations.DiscriminatorOptions;

/**
 * @author Hardy Ferentschik
 */
@Entity
@DiscriminatorValue("B")
@DiscriminatorOptions(force = true, insert = false)
public class BaseClass {
	@Id
	@GeneratedValue
	private long id;
}

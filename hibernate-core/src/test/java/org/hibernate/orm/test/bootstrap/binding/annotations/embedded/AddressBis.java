/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.bootstrap.binding.annotations.embedded;

import jakarta.persistence.Embeddable;
import org.hibernate.annotations.Formula;

@Embeddable
public class AddressBis {

	@Formula("2")
	Integer formula;
}

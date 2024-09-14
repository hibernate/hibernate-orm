/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.derivedidentities.e3.b3;

import jakarta.persistence.*;

@Entity
@Table(name="`Dependent`")
public class Dependent {

	@EmbeddedId
	DependentId id;

	@JoinColumn(name = "FIRSTNAME", referencedColumnName = "FIRSTNAME")
	@JoinColumn(name = "LASTNAME", referencedColumnName = "lastName")
	@MapsId("empPK")
	@ManyToOne
	Employee emp;
}

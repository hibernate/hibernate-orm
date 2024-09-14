/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.derivedidentities.e3.b3;

import jakarta.persistence.*;

@Entity
@Table(name="`Policy`")
public class Policy {
	@EmbeddedId
	PolicyId id;

	@JoinColumn(name = "FIRSTNAME", referencedColumnName = "FIRSTNAME")
	@JoinColumn(name = "LASTNAME", referencedColumnName = "lastName")
	@JoinColumn(name = "NAME", referencedColumnName = "Name")
	@MapsId("depPK")
	@ManyToOne
	Dependent dep;

}

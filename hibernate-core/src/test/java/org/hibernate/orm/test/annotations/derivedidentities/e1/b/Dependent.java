/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.derivedidentities.e1.b;
import jakarta.persistence.CascadeType;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Dependent {
	@EmbeddedId
	DependentId id;

	//@JoinColumn(name="FK") // id attribute mapped by join column default
	@MapsId("empPK") // maps empPK attribute of embedded id
	@ManyToOne(cascade = CascadeType.PERSIST)
	@JoinColumn(nullable=false)
	Employee emp;

}

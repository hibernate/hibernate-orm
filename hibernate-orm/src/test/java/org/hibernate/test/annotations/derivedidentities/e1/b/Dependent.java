/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.derivedidentities.e1.b;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Dependent {
	@EmbeddedId
	DependentId id;

	//@JoinColumn(name="FK") // id attribute mapped by join column default
	@MapsId("empPK") // maps empPK attribute of embedded id
	@ManyToOne
	@JoinColumn(nullable=false)
	Employee emp;

}

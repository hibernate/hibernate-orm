/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.derivedidentities.e1.b;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class ExclusiveDependent {
	@EmbeddedId
	DependentId id;

	@JoinColumn(name = "FK", nullable = false)
	// id attribute mapped by join column default
	@MapsId("empPK")
	// maps empPK attribute of embedded id
	@OneToOne
	Employee emp;
}

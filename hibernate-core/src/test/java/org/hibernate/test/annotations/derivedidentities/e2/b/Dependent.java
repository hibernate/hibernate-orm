/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.derivedidentities.e2.b;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Dependent {
	@EmbeddedId
	DependentId id;

	@MapsId("empPK")

    @ManyToOne
    @JoinColumns( { @JoinColumn(nullable = false), @JoinColumn(nullable = false) })
    Employee emp;
}

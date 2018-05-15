/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.derivedidentities.e6.b;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.MapsId;
import javax.persistence.OneToOne;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class MedicalHistory {
	//all attributes map to relationship: AttributeOverride not allowed
	@EmbeddedId
	PersonId id;

	@MapsId
	@JoinColumns({
			@JoinColumn(name = "FK1", referencedColumnName = "firstName"),
			@JoinColumn(name = "FK2", referencedColumnName = "lastName")
	})
	@OneToOne
	Person patient;
}

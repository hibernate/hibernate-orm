/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.derivedidentities.e5.a;
import java.io.Serializable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.OneToOne;

/**
 * @author Emmanuel Bernard
 */
@Entity
@IdClass(PersonId.class)
public class MedicalHistory implements Serializable {
	@Id
	@JoinColumns({
			@JoinColumn(name = "FK1", referencedColumnName = "firstName"),
			@JoinColumn(name = "FK2", referencedColumnName = "lastName")
	})
	@OneToOne
	Person patient;

	public MedicalHistory() {
	}

	public MedicalHistory(Person patient) {
		this.patient = patient;
	}
}

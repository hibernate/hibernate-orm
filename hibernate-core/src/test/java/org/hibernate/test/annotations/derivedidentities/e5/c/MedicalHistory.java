/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.derivedidentities.e5.c;
import java.io.Serializable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;


/**
 * @author Emmanuel Bernard
 */
@Entity
public class MedicalHistory implements Serializable {
	@Id
	Integer id;

	@MapsId
	@JoinColumn(name = "patient_id")
	@OneToOne
	Person patient;
}

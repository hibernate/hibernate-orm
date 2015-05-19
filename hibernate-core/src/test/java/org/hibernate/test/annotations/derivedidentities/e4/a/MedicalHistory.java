/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.derivedidentities.e4.a;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;


/**
 * @author Emmanuel Bernard
 */
@Entity
public class MedicalHistory implements Serializable {
	@Id
	@JoinColumn(name = "FK")
	@OneToOne
	Person patient;

	@Temporal(TemporalType.DATE)
	Date lastupdate;

	public MedicalHistory() {
	}

	public MedicalHistory(Person patient) {
		this.patient = patient;
	}
}

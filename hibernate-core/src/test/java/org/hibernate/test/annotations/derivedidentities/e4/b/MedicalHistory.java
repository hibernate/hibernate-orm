/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.derivedidentities.e4.b;
import java.util.Date;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MapsId;
import javax.persistence.OneToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class MedicalHistory {

	@Id String id; // overriding not allowed ... // default join column name is overridden @MapsId

	@MapsId
	@JoinColumn(name = "FK")
    @OneToOne(cascade= CascadeType.ALL)
	Person patient;

	@Temporal(TemporalType.DATE)
	Date lastupdate;

	public MedicalHistory() {
	}

	public MedicalHistory(String id, Person patient) {
		this.id = id;
		this.patient = patient;
	}
}

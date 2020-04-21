/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.derivedidentities.e4.a;
import java.io.Serializable;
import java.util.Date;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;


/**
 * @author Emmanuel Bernard
 */
@Entity
public class FinancialHistory implements Serializable {
	@Id
	//@JoinColumn(name = "FK")
	@ManyToOne
	Person patient;

	@Temporal(TemporalType.DATE)
	Date lastUpdate;

	public FinancialHistory() {
	}

	public FinancialHistory(Person patient) {
		this.patient = patient;
	}
}

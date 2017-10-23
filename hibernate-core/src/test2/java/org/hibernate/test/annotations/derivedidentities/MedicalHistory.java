/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

// $Id:$

package org.hibernate.test.annotations.derivedidentities;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.OneToOne;

import org.hibernate.annotations.Entity;

/**
 * @author Hardy Ferentschik
 */
@Entity
public class MedicalHistory {
	@Id
	@OneToOne
	@JoinColumn(name="FK")
	Person patient;

	@Lob
	byte[] xrayData;

	private MedicalHistory() {
	}

	public MedicalHistory(Person patient) {
		this.patient = patient;
	}

	public byte[] getXrayData() {
		return xrayData;
	}

	public void setXrayData(byte[] xrayData) {
		this.xrayData = xrayData;
	}
}



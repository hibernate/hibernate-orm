// $Id:$
/*
* JBoss, Home of Professional Open Source
* Copyright 2009, Red Hat, Inc. and/or its affiliates, and individual contributors
* by the @authors tag. See the copyright.txt in the distribution for a
* full listing of individual contributors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.hibernate.test.annotations.derivedidentities;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.OneToOne;


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



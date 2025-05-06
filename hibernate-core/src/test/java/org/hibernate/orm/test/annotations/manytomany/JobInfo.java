/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.manytomany;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.ManyToOne;

@Embeddable
public class JobInfo {
String jobDescription;
	ProgramManager pm; // Bidirectional

	public String getJobDescription() {
		return jobDescription;
	}

	public void setJobDescription( String jobDescription ) {
		this.jobDescription = jobDescription;
	}

	@ManyToOne( cascade= CascadeType.ALL)
	public ProgramManager getPm() {
		return pm;
	}

	public void setPm( ProgramManager pm ) {
		this.pm = pm;
	}

}

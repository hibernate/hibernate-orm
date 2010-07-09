package org.hibernate.test.annotations.manytomany;

import javax.persistence.CascadeType;
import javax.persistence.Embeddable;
import javax.persistence.ManyToOne;

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

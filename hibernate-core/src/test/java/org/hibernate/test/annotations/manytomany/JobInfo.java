/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
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

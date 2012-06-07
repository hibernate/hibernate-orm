// $Id: JobBatch.java 6663 2005-05-03 20:55:31Z steveebersole $
package org.hibernate.test.cascade;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * Implementation of JobBatch.
 *
 * @author Steve Ebersole
 */
public class JobBatch {
	private Long id;
	private Date batchDate;
	private Set jobs = new HashSet();

	/** CGLIB constructor */
	JobBatch() {}

	public JobBatch(Date batchDate) {
		this.batchDate = batchDate;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Date getBatchDate() {
		return batchDate;
	}

	public void setBatchDate(Date batchDate) {
		this.batchDate = batchDate;
	}

	public Set getJobs() {
		return jobs;
	}

	public void setJobs(Set jobs) {
		this.jobs = jobs;
	}

	public Job createJob() {
		Job job = new Job( this );
		jobs.add( job );
		return job;
	}
}

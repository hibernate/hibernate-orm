/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

// $Id: Job.java 6663 2005-05-03 20:55:31Z steveebersole $
package org.hibernate.test.cascade;


/**
 * Implementation of Job.
 *
 * @author Steve Ebersole
 */
public class Job {
	private Long id;
	private JobBatch batch;
	private String processingInstructions;
	private int status;

	/** GCLIB constructor */
	Job() {}

	protected Job(JobBatch batch) {
		this.batch = batch;
	}

	public Long getId() {
		return id;
	}

	/*package*/ void setId(Long id) {
		this.id = id;
	}

	public JobBatch getBatch() {
		return batch;
	}

	/*package*/ void setBatch(JobBatch batch) {
		this.batch = batch;
	}

	public String getProcessingInstructions() {
		return processingInstructions;
	}

	public void setProcessingInstructions(String processingInstructions) {
		this.processingInstructions = processingInstructions;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}
}

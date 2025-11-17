/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.collectionelement;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class Bug {

	private String description;
	private Person reportedBy;
	private String summary;

	@Column(name="`summary`")
	public String getSummary() {
		return summary;
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}

	public Person getReportedBy() {
		return reportedBy;
	}

	public void setReportedBy(Person reportedBy) {
		this.reportedBy = reportedBy;
	}

	public String getDescription(){
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}

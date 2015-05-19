/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.collectionelement;
import javax.persistence.Column;
import javax.persistence.Embeddable;

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

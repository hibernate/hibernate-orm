/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.join;

/**
 * @author Gail Badner
 */
public abstract class Reportable {
	private Long id;
	private String reportedBy;

	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getReportedBy() {
		return reportedBy;
	}
	public void setReportedBy(String reportedBy) {
		this.reportedBy = reportedBy;
	}
}

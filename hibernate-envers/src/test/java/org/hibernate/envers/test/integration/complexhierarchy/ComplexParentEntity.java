/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.complexhierarchy;

import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;

/**
 * @author Naros (crancran at gmail dot com)
 */
@Entity
@Table(name = "parent")
@Audited
public class ComplexParentEntity {
	
	@Id
	private Long id;
	
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(nullable = false)
	private ChildEntity child;
	
	@OneToMany(mappedBy = "security", cascade = CascadeType.ALL)
	private List<ChildDiscrepancy> discrepancyList;
	
	@Temporal(TemporalType.DATE)
	private Date issueDate;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public ChildEntity getChild() {
		return child;
	}

	public void setChild(ChildEntity child) {
		this.child = child;
	}

	public List<ChildDiscrepancy> getDiscrepancyList() {
		return discrepancyList;
	}

	public void setDiscrepancyList(List<ChildDiscrepancy> discrepancyList) {
		this.discrepancyList = discrepancyList;
	}

	public Date getIssueDate() {
		return issueDate;
	}

	public void setIssueDate(Date issueDate) {
		this.issueDate = issueDate;
	}
	
}

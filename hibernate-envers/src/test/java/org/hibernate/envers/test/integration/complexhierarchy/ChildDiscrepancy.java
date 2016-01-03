/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.complexhierarchy;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.envers.Audited;

/**
 * @author Naros (crancran at gmail dot com)
 */
@Entity
@Table(name = "child_discrepancy")
@Audited
public class ChildDiscrepancy {

	@Id
	private Long id;
	
	private String leadingAttributeName;
	
	@ManyToOne(cascade = CascadeType.REFRESH)
	@JoinColumn(nullable = false)
	private ComplexParentEntity security;
	
	private String targetEntityName;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getLeadingAttributeName() {
		return leadingAttributeName;
	}

	public void setLeadingAttributeName(String leadingAttributeName) {
		this.leadingAttributeName = leadingAttributeName;
	}

	public ComplexParentEntity getSecurity() {
		return security;
	}

	public void setSecurity(ComplexParentEntity security) {
		this.security = security;
	}

	public String getTargetEntityName() {
		return targetEntityName;
	}

	public void setTargetEntityName(String targetEntityName) {
		this.targetEntityName = targetEntityName;
	}
	
	@Override
	public int hashCode() {
		int result;
		result = (id != null ? id.hashCode() : 0);
		result = 31 * result + (targetEntityName != null ? targetEntityName.hashCode() : 0);
		result = 31 * result + (leadingAttributeName != null ? leadingAttributeName.hashCode() : 0);
		result = 31 * result + (security != null ? security.hashCode() : 0);
		return result;
	}
	
}

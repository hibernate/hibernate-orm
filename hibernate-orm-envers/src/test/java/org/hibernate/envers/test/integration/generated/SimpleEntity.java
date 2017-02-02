/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.generated;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;
import org.hibernate.envers.Audited;

/**
 * @author Chris Cranford
 */
@Entity
@Audited
public class SimpleEntity {
	@Id
	@GeneratedValue
	private Integer id;

	private String data;

	@Generated(GenerationTime.INSERT)
	@Column(columnDefinition = "integer default 1")
	private int caseNumberInsert;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	public int getCaseNumberInsert() {
		return caseNumberInsert;
	}

	public void setCaseNumberInsert(int caseNumberInsert) {
		this.caseNumberInsert = caseNumberInsert;
	}
}

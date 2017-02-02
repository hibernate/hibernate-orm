/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.embeddables;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;

/**
 * @author Chris Pheby
 */
@Entity
public class Investor {
	
	private Long id;
	
	private List<Investment> investments = new ArrayList<Investment>();

	@Id
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
	
	@ElementCollection(fetch = FetchType.EAGER)
	public List<Investment> getInvestments() {
		return investments;
	}

	public void setInvestments(List<Investment> investments) {
		this.investments = investments;
	}
}

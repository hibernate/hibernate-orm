/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.test.annotations.inheritance.joined;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class ProgramExecution {
	@Id
	@GeneratedValue
	private Integer id;
	private String action;
	@ManyToOne(fetch = FetchType.LAZY)
	private File appliesOn;


	public File getAppliesOn() {
		return appliesOn;
	}

	public void setAppliesOn(File appliesOn) {
		this.appliesOn = appliesOn;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}


	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}
}

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.ids.embeddedid;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import org.hibernate.envers.Audited;

/**
 * @author Chris Cranford
 */
@Audited
@Entity
public class Parent {
	@Id
	private String id;

	@OneToMany(mappedBy = "parent")
	private List<CorrectChild> correctChildren = new ArrayList<CorrectChild>();

	@OneToMany(mappedBy = "id.parent")
	private List<IncorrectChild> incorrectChildren = new ArrayList<IncorrectChild>();

	Parent() {

	}

	Parent(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public List<CorrectChild> getCorrectChildren() {
		return correctChildren;
	}

	public void setCorrectChildren(List<CorrectChild> correctChildren) {
		this.correctChildren = correctChildren;
	}

	public List<IncorrectChild> getIncorrectChildren() {
		return incorrectChildren;
	}

	public void setIncorrectChildren(List<IncorrectChild> incorrectChildren) {
		this.incorrectChildren = incorrectChildren;
	}

	public void addIncorrectChild(Integer number) {
		this.incorrectChildren.add( new IncorrectChild( number, this ) );
	}

	public void addCorrectChild(Integer number) {
		this.correctChildren.add( new CorrectChild( number, this ) );
	}
}

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.dirty;

import java.util.List;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * @author Vlad Mihalcea
 */
@Entity
public class ParentChildEntity {

	@Id
	private Long id;

	@ElementCollection
	private List<String> someStrings;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public List<String> getSomeStrings() {
		return someStrings;
	}

	public void setSomeStrings(List<String> someStrings) {
		this.someStrings = someStrings;
	}
}

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.test.annotations.inheritance.joined;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.OneToMany;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Folder extends File {
	@OneToMany(mappedBy = "parent")
	private Set<File> children = new HashSet<File>();

	Folder() {
	}

	public Folder(String name) {
		super( name );
	}

	public Set<File> getChildren() {
		return children;
	}

	public void setChildren(Set children) {
		this.children = children;
	}
}

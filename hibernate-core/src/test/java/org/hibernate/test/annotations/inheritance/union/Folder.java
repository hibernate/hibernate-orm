//$Id$
package org.hibernate.test.annotations.inheritance.union;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Table(name = "FolderUnion")
public class Folder extends File {
	private Set<File> children = new HashSet<File>();

	Folder() {
	}

	public Folder(String name) {
		super( name );
	}

	@OneToMany(mappedBy = "parent")
	public Set<File> getChildren() {
		return children;
	}

	public void setChildren(Set children) {
		this.children = children;
	}
}

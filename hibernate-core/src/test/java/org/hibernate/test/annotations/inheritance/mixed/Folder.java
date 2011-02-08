//$Id$
package org.hibernate.test.annotations.inheritance.mixed;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.OneToMany;

/**
 * @author Emmanuel Bernard
 */
@Entity
@DiscriminatorValue("F")
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

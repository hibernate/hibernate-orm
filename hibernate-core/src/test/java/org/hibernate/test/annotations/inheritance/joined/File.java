//$Id$
package org.hibernate.test.annotations.inheritance.joined;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name="joined_file")
public abstract class File {
	@Id @Column(name="filename")
	private String name;
	@ManyToOne
	private Folder parent;

	File() {
	}

	public File(String name) {
		this.name = name;
	}


	public String getName() {
		return name;
	}

	public void setName(String id) {
		this.name = id;
	}

	public Folder getParent() {
		return parent;
	}

	public void setParent(Folder parent) {
		this.parent = parent;
	}

}

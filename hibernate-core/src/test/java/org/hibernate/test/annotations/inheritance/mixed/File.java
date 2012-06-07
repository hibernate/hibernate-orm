//$Id$
package org.hibernate.test.annotations.inheritance.mixed;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SecondaryTable;
import javax.persistence.Table;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Table(name = "FileMixed")
@SecondaryTable(name = "FileFolderMixed")
@DiscriminatorColumn(length = 1)
public abstract class File {
	private String name;
	private Folder parent;

	File() {
	}

	public File(String name) {
		this.name = name;
	}

	@Id
	public String getName() {
		return name;
	}

	public void setName(String id) {
		this.name = id;
	}

	@ManyToOne
	@JoinColumn(table = "FileFolderMixed")
	public Folder getParent() {
		return parent;
	}

	public void setParent(Folder parent) {
		this.parent = parent;
	}

}

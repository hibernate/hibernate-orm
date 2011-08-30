//$Id$
package org.hibernate.test.annotations.inheritance.mixed;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SecondaryTable;

@Entity
@DiscriminatorValue("L")
@SecondaryTable(name = "SymbolicLinkMixed")
public class SymbolicLink extends File {

	File target;

	SymbolicLink() {
	}

	public SymbolicLink(File target) {
		this.target = target;
	}

	@ManyToOne(optional = false)
	@JoinColumn(table = "SymbolicLinkMixed")
	public File getTarget() {
		return target;
	}

	public void setTarget(File target) {
		this.target = target;
	}


}

//$Id$
package org.hibernate.test.annotations.inheritance.mixed;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.SecondaryTable;
import javax.persistence.Table;

/**
 * @author Emmanuel Bernard
 */
@Entity
@DiscriminatorValue("D")
@SecondaryTable(name = "DocumentMixed")
@Table(name="Document_Wrong") //illegal use, a warn is raised
public class Document extends File {
	private int size;

	Document() {
	}

	Document(String name, int size) {
		super( name );
		this.size = size;
	}

	@Column(table = "DocumentMixed", name="doc_size", nullable = false)
	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}
}

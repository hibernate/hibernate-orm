//$
package org.hibernate.test.annotations.collectionelement.deepcollectionelements;

/**
 * @author Emmanuel Bernard
 */

import java.util.List;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.hibernate.annotations.CollectionOfElements;
import org.hibernate.annotations.IndexColumn;

@Entity
@Table( name = "A" )
public class A {
	@Id
	@GeneratedValue( strategy = GenerationType.SEQUENCE, generator = "aSequence" )
	@SequenceGenerator( name = "aSequence", sequenceName = "seq_A" )
	private int id;
	@CollectionOfElements
	@IndexColumn( name = "ndx" )
	private List<B> listOfB;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public List<B> getListOfB() {
		return listOfB;
	}

	public void setListOfB(List<B> listOfB) {
		this.listOfB = listOfB;
	}
}

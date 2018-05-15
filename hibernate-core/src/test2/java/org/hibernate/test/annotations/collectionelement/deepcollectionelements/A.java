/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$
package org.hibernate.test.annotations.collectionelement.deepcollectionelements;
import java.util.List;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OrderColumn;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

@Entity
@Table( name = "A" )
public class A {
	@Id
	@GeneratedValue( strategy = GenerationType.SEQUENCE, generator = "aSequence" )
	@SequenceGenerator( name = "aSequence", sequenceName = "seq_A" )
	private int id;
	@ElementCollection
	@OrderColumn( name = "ndx" )
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

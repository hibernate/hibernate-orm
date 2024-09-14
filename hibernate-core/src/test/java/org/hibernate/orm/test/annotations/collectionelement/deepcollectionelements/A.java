/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.collectionelement.deepcollectionelements;
import java.util.List;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

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

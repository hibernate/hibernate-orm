//$
package org.hibernate.test.annotations.collectionelement.deepcollectionelements;

import java.util.List;
import javax.persistence.Embeddable;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;

@Embeddable
public class B {
	private String name;

	//@CollectionOfElements
	@OneToMany
	@OrderColumn( name = "ndx" )
	private List<C> listOfC;

	public List<C> getListOfC() {
		return listOfC;
	}

	public void setListOfC(List<C> listOfC) {
		this.listOfC = listOfC;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}

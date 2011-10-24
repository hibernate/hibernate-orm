//$Id$
package org.hibernate.test.annotations.inheritance;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.PrimaryKeyJoinColumns;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@PrimaryKeyJoinColumns(
		{@PrimaryKeyJoinColumn(name = "carrot_farmer", referencedColumnName = "farmer"),
		@PrimaryKeyJoinColumn(name = "harvest", referencedColumnName = "harvestDate")
				})
@OnDelete(action = OnDeleteAction.CASCADE)
public class Carrot extends Vegetable {
	private int length;

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}
}

//$
package org.hibernate.test.annotations.collectionelement.deepcollectionelements;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;


@Entity
public class C {
	@Id
	@GeneratedValue
	private int id;

	@Column( length = 500 )
	private String comment;

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}
}

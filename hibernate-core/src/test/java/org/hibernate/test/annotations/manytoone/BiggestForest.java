//$Id$
package org.hibernate.test.annotations.manytoone;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class BiggestForest {
	private Integer id;
	private ForestType type;

	@Id @GeneratedValue
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@OneToOne(mappedBy = "biggestRepresentative")
	public ForestType getType() {
		return type;
	}

	public void setType(ForestType type) {
		this.type = type;
	}
}

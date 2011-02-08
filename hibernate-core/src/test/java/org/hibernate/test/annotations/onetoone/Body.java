//$Id$
package org.hibernate.test.annotations.onetoone;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Body {
	private Integer id;
	private Heart heart;

	@OneToOne
	@PrimaryKeyJoinColumn
	public Heart getHeart() {
		return heart;
	}

	public void setHeart(Heart heart) {
		this.heart = heart;
	}

	@Id
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}
}

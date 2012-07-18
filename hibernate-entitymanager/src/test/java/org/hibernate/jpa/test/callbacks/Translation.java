//$Id$
package org.hibernate.jpa.test.callbacks;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Table(name = "Translatn")
@CountryChecker
public class Translation {
	private Integer id;
	private String into;

	@Id
	@GeneratedValue
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Column(name = "country_into")
	public String getInto() {
		return into;
	}

	public void setInto(String into) {
		this.into = into;
	}
}

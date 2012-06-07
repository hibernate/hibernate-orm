//$Id$
package org.hibernate.test.annotations.embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class InternetProvider {
	private Integer id;
	private String brandName;
	private LegalStructure owner;

	public String getBrandName() {
		return brandName;
	}

	public void setBrandName(String brandName) {
		this.brandName = brandName;
	}

	@Id
	@GeneratedValue
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public LegalStructure getOwner() {
		return owner;
	}

	public void setOwner(LegalStructure owner) {
		this.owner = owner;
	}
}

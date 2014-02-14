//$Id$
package org.hibernate.test.annotations.tableperclass;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;

/**
 * @author Emmanuel Bernard
 */
@Entity(name = "xpmComponent")
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@Table(indexes = @Index(name = "manufacturerPartNumber", columnList = "manufacturerPartNumber"))
public abstract class Component {
	private String manufacturerPartNumber;
	private Long manufacturerId;
	private Long id;



	public void setId(Long id) {
		this.id = id;
	}


	@Id
	public Long getId() {
		return id;
	}

	@Column(nullable = false)
	public String getManufacturerPartNumber() {
		return manufacturerPartNumber;
	}

	@Column(nullable = false)
	public Long getManufacturerId() {
		return manufacturerId;
	}

	public void setManufacturerId(Long manufacturerId) {
		this.manufacturerId = manufacturerId;
	}


	public void setManufacturerPartNumber(String manufacturerPartNumber) {
		this.manufacturerPartNumber = manufacturerPartNumber;
	}
}

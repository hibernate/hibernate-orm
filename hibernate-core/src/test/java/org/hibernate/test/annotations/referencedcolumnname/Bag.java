//$Id$
package org.hibernate.test.annotations.referencedcolumnname;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Bag {
	private Integer id;
	private String serial;
	private Rambler owner;

	public Bag() {
	}

	public Bag(String serial, Rambler owner) {
		this.serial = serial;
		this.owner = owner;
	}

	@Id
	@GeneratedValue
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Column(unique = true)
	public String getSerial() {
		return serial;
	}

	public void setSerial(String serial) {
		this.serial = serial;
	}

	@ManyToOne
	@JoinColumn(referencedColumnName = "fld_name")
	public Rambler getOwner() {
		return owner;
	}

	public void setOwner(Rambler owner) {
		this.owner = owner;
	}
}

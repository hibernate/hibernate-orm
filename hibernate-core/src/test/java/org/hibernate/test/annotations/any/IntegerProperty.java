package org.hibernate.test.annotations.any;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Column;

@Entity
@Table(name="int_property")
public class IntegerProperty implements Property {
	private Integer id;
	private String name;
    @Column(name = "`value`")
	private Integer value;
	
	public IntegerProperty() {
		super();
	}

	public IntegerProperty(String name, Integer value) {
		super();
		this.name = name;
		this.value = value;
	}

	public String asString() {
		return Integer.toString(value);
	}

	public String getName() {
		return name;
	}

	@Id
	@GeneratedValue
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Integer getValue() {
		return value;
	}

	public void setValue(Integer value) {
		this.value = value;
	}

	public void setName(String name) {
		this.name = name;
	}

	
}

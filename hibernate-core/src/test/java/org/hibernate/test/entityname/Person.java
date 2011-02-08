package org.hibernate.test.entityname;
import java.util.HashSet;
import java.util.Set;
/**
 * 
 * @author stliu
 *
 */
public class Person {
	private Long id;
	private String Name;
	private Set cars = new HashSet();

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return Name;
	}

	public void setName(String name) {
		Name = name;
	}

	public Set getCars() {
		return cars;
	}

	public void setCars(Set cars) {
		this.cars = cars;
	}

}

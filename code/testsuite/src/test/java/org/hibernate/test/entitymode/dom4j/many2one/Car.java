package org.hibernate.test.entitymode.dom4j.many2one;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Paco Hernández
 */
public class Car implements java.io.Serializable {

	private long id;
	private String model;
	private CarType carType;
	private Set carParts = new HashSet();
	
	/**
	 * @return Returns the carType.
	 */
	public CarType getCarType() {
		return carType;
	}
	/**
	 * @param carType The carType to set.
	 */
	public void setCarType(CarType carType) {
		this.carType = carType;
	}
	/**
	 * @return Returns the id.
	 */
	public long getId() {
		return id;
	}
	/**
	 * @param id The id to set.
	 */
	public void setId(long id) {
		this.id = id;
	}
	/**
	 * @return Returns the model.
	 */
	public String getModel() {
		return model;
	}
	/**
	 * @param model The model to set.
	 */
	public void setModel(String model) {
		this.model = model;
	}
	public Set getCarParts() {
		return carParts;
	}
	public void setCarParts(Set carParts) {
		this.carParts = carParts;
	}
}

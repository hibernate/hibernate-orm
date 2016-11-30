package org.hibernate.test.proxy.narrow;

/**
 * @author jlandin
 */
public class User {
	private Long id;
	private Integer type;

	/**
	 * Constructs a new User.
	 */
	public User() {
		super();
	}

	/**
	 * Retrieves the value of the id property.
	 * @return The value of the id property.
	 */
	public Long getId() {
		return this.id;
	}

	/**
	 * Sets the value of the id property.
	 * @param id The value to set for the property id.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Retrieves the value of the type property.
	 * @return The value of the type property.
	 */
	public Integer getType() {
		return this.type;
	}

	/**
	 * Sets the value of the type property.
	 * @param type The value to set for the property type.
	 */
	public void setType(Integer type) {
		this.type = type;
	}

}
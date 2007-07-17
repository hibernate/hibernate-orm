package org.hibernate.test.entitymode.dom4j.many2one;

/**
 * @author Paco Hernández
 */
public class CarType implements java.io.Serializable {

	private long id;
	private String typeName;

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
	 * @return Returns the typeName.
	 */
	public String getTypeName() {
		return typeName;
	}
	/**
	 * @param typeName The typeName to set.
	 */
	public void setTypeName(String typeName) {
		this.typeName = typeName;
	}
}

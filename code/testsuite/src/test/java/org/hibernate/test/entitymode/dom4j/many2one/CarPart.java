package org.hibernate.test.entitymode.dom4j.many2one;

/**
 * @author Paco Hernández
 */
public class CarPart implements java.io.Serializable {

	private long id;
	private String partName;

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
	public String getPartName() {
		return partName;
	}
	/**
	 * @param typeName The typeName to set.
	 */
	public void setPartName(String typeName) {
		this.partName = typeName;
	}
}

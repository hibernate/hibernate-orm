//$Id: SessionAttribute.java 5686 2005-02-12 07:27:32Z steveebersole $
package org.hibernate.test.map;

import java.io.Serializable;

/**
 * @author Gavin King
 */
public class SessionAttribute {
	private Long id;
	private String name;
	private String stringData;
	private Serializable objectData;
	SessionAttribute() {}
	public SessionAttribute(String name, Serializable obj) {
		this.name = name;
		this.objectData = obj;
	}
	public SessionAttribute(String name, String str) {
		this.name = name;
		this.stringData = str;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Serializable getObjectData() {
		return objectData;
	}
	public void setObjectData(Serializable objectData) {
		this.objectData = objectData;
	}
	public String getStringData() {
		return stringData;
	}
	public void setStringData(String stringData) {
		this.stringData = stringData;
	}
}

//$Id: SessionAttribute.java 7628 2005-07-24 06:55:01Z oneovthafew $
package org.hibernate.test.extralazy;

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

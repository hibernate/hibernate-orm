package org.hibernate.test.abstractembeddedcomponents.cid;



/**
 * @author Steve Ebersole
 */
public class MyInterfaceImpl implements MyInterface {
	private String key1;
	private String key2;
	private String name;

	public String getKey1() {
		return key1;
	}

	public void setKey1(String key1) {
		this.key1 = key1;
	}

	public String getKey2() {
		return key2;
	}

	public void setKey2(String key2) {
		this.key2 = key2;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}

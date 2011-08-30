package org.hibernate.test.abstractembeddedcomponents.cid;
import java.io.Serializable;

/**
 * @author Steve Ebersole
 */
public interface MyInterface extends Serializable {
	public String getKey1();
	public void setKey1(String key1);
	public String getKey2();
	public void setKey2(String key2);
	public String getName();
	public void setName(String name);
}

package org.hibernate.test.legacy;
import java.util.Map;


public class B extends A {
	private int count;
	private Map map;
	private String bName = "B Name";
	
	public int getCount() {
		return count;
	}
	
	public void setCount(int count) {
		this.count = count;
	}
	
	public Map getMap() {
		return map;
	}

	public void setMap(Map map) {
		this.map = map;
	}

	public String getBName() {
		return bName;
	}

	public void setBName(String name) {
		bName = name;
	}
}







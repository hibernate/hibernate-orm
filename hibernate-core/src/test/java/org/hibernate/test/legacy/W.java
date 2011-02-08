package org.hibernate.test.legacy;
import java.util.Set;

public class W {
	
	private long id;
	private Set zeds;
	
	/**
	 * 
	 */
	public W() {
	}

	/**
	 * @return
	 */
	public long getId() {
		return id;
	}

	/**
	 * @return
	 */
	public Set getZeds() {
		return zeds;
	}

	/**
	 * @param l
	 */
	public void setId(long l) {
		id = l;
	}

	/**
	 * @param set
	 */
	public void setZeds(Set set) {
		zeds = set;
	}

}

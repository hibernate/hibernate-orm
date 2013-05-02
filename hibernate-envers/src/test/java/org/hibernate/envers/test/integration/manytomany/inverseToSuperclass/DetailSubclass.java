package org.hibernate.envers.test.integration.manytomany.inverseToSuperclass;

import org.hibernate.envers.Audited;

@Audited
public class DetailSubclass extends DetailSuperclass {

	private String str2;

	public DetailSubclass() {

	}

	public String getStr2() {
		return str2;
	}

	public void setStr2(String str2) {
		this.str2 = str2;
	}

}

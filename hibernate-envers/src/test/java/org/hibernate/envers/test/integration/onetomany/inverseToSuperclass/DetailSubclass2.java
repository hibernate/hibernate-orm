package org.hibernate.envers.test.integration.onetomany.inverseToSuperclass;

import org.hibernate.envers.Audited;

@Audited
public class DetailSubclass2 extends DetailSubclass {

	private String str3;

	public DetailSubclass2() {

	}

	public String getStr3() {
		return str3;
	}

	public void setStr3(String str3) {
		this.str3 = str3;
	}

}

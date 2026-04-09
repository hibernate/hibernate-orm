package org.hibernate.tool.hbm2x.query.QueryExporterTest;

public class UserID {

	private String org;
	private String name;
	
	public UserID() {}

	public UserID(String name, String org) {
		this.org = org;
		this.name = name;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getOrg() {
		return org;
	}

	public void setOrg(String org) {
		this.org = org;
	}

}

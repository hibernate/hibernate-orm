package org.hibernate.tool.gradle;

import org.gradle.api.Project;

public class Extension {
	
	private String sql = "";
	
	public Extension(Project project) {}
	
	public String getSql() {
		return sql;
	}
	
	public void setSql(String sql) {
		this.sql = sql;
	}

}

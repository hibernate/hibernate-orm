package org.hibernate.orm.test.boot.jaxb.mapping.timestampsource;

import java.util.Date;

public class VmSourcedEntity {
	private Long id;
	private Date lastModified;
	private String name;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Date getLastModified() {
		return lastModified;
	}

	public void setLastModified(Date lastModified) {
		this.lastModified = lastModified;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}

package org.hibernate.test.fetchprofiles.cycle;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
@Entity
public class Finish {
	@Id
	@GeneratedValue
	private long id;

	@Column(name="sumdumattr", nullable=false)
	private String sumdumattr;
	
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getSumdumattr() {
		return sumdumattr;
	}

	public void setSumdumattr(String sumdumattr) {
		this.sumdumattr = sumdumattr;
	}
}

package org.hibernate.jpa.test.metamodel;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class MapEntityLocal {
	
	@Column(name="short_name") 
	private String shortName;
	
	public String getShortName() {
		return shortName;
	}
	
	public void setShortName(String shortName) {
		this.shortName = shortName;
	}
}

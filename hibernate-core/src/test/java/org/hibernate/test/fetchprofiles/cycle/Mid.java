package org.hibernate.test.fetchprofiles.cycle;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.FetchProfile;
import org.hibernate.annotations.FetchProfile.FetchOverride;
@Entity
@FetchProfile(name="fp", fetchOverrides={@FetchOverride(entity=Mid.class, association="finish", mode=FetchMode.JOIN)})
public class Mid {
	@Id
	@GeneratedValue
	private long id;

	
	@ManyToOne(optional=false, fetch=FetchType.LAZY)
	private Finish finish;

	
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public Finish getFinish() {
		return finish;
	}

	public void setFinish(Finish finish) {
		this.finish = finish;
	}
	
}

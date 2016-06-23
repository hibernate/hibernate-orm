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
@FetchProfile(name="fp", fetchOverrides={@FetchOverride(entity=Via1.class, association="mid", mode=FetchMode.JOIN)})

public class Via1 {
	@Id
	@GeneratedValue
	private long id;

	@ManyToOne(optional=true, fetch=FetchType.LAZY)
	private Mid mid;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public Mid getMid() {
		return mid;
	}

	public void setMid(Mid mid) {
		this.mid = mid;
	}
	
	
}

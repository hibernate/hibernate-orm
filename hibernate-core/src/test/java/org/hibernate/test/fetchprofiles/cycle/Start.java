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
@FetchProfile(name="fp", fetchOverrides=
	{@FetchOverride(entity=Start.class, association="via1", mode=FetchMode.JOIN),
	@FetchOverride(entity=Start.class, association="via2", mode=FetchMode.JOIN)}
)
public class Start {
	@Id
	@GeneratedValue
	private long id;

	@ManyToOne(optional=true, fetch=FetchType.LAZY)
	private Via1 via1;
	
	@ManyToOne(optional=true, fetch=FetchType.LAZY)
	private Via2 via2;
	
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public Via1 getVia1() {
		return via1;
	}

	public void setVia1(Via1 via1) {
		this.via1 = via1;
	}

	public Via2 getVia2() {
		return via2;
	}

	public void setVia2(Via2 via2) {
		this.via2 = via2;
	}
	
	
	
}

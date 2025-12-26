package org.hibernate.orm.test.joinformulamappedby;

import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.FetchProfile;
import org.hibernate.annotations.FetchProfiles;
import org.hibernate.annotations.Where;

@Entity
@Table(name="TEST_LINK")
public class TESTLink {

	@Id
	@Column(name = "LINK_ID", nullable = false)
	private long id;

	@Column(name = "NAME", nullable = false, length = 255)
	private String name;

        @Where(clause = "entity_class = 'org.hibernate.orm.test.joinformulamappedby.TESTLink'")
	@OneToMany(fetch = FetchType.LAZY, mappedBy = "link")
	private Set<TESTAttribute> attributes;
	
	protected TESTLink()
	{	

	}

	
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}


	
}

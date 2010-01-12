package org.hibernate.test.annotations.collectionelement;

import javax.persistence.CascadeType;
import javax.persistence.Embeddable;
import javax.persistence.ManyToMany;
import java.util.List;

@Embeddable
public class SocialTouchPoints {

	// owning side of many to many
	@ManyToMany(targetEntity=SocialSite.class, cascade= CascadeType.ALL)
	List<SocialSite> website;

	public List<SocialSite> getWebsite() {
		return website;
	}

	public void setWebsite(List<SocialSite> website) {
		this.website = website;
	}
}

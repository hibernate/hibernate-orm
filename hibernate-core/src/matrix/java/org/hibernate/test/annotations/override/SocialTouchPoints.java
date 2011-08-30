package org.hibernate.test.annotations.override;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Embeddable;
import javax.persistence.ManyToMany;

@Embeddable
public class SocialTouchPoints {

	// owning side of many to many
	@ManyToMany(cascade= CascadeType.ALL)
	List<SocialSite> website;

	public List<SocialSite> getWebsite() {
		return website;
	}

	public void setWebsite(List<SocialSite> website) {
		this.website = website;
	}
}

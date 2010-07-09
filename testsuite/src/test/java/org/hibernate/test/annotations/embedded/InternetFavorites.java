package org.hibernate.test.annotations.embedded;

import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import java.util.Collection;

@Embeddable
public class InternetFavorites {

	@Embedded
	Collection<URLFavorite> links;

	@Embedded
	Collection<String> ideas;

	public Collection<String> getIdeas() {
		return ideas;
	}

	public void setIdeas(Collection<String> ideas) {
		this.ideas = ideas;
	}

	public Collection<URLFavorite> getLinks() {

		return links;
	}

	public void setLinks(Collection<URLFavorite> links) {
		this.links = links;
	}
}

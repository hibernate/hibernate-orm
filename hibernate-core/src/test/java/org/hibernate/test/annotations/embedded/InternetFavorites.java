package org.hibernate.test.annotations.embedded;
import java.util.Collection;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;

@Embeddable
public class InternetFavorites {

	@ElementCollection
	Collection<URLFavorite> links;

	@ElementCollection
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

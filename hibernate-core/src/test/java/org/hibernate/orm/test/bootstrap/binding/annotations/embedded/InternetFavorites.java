/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.binding.annotations.embedded;
import java.util.Collection;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;

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

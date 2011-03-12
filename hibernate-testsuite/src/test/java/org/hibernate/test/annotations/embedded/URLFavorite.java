package org.hibernate.test.annotations.embedded;

import javax.persistence.Embeddable;

@Embeddable
public class URLFavorite {

	private String url;

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}
}

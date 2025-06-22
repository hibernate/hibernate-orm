/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.ordered;
import java.util.HashSet;
import java.util.Set;

public class Search {
	private String searchString;
	private Set searchResults = new HashSet();

	Search() {}

	public Search(String string) {
		searchString = string;
	}

	public Set getSearchResults() {
		return searchResults;
	}
	public void setSearchResults(Set searchResults) {
		this.searchResults = searchResults;
	}
	public String getSearchString() {
		return searchString;
	}
	public void setSearchString(String searchString) {
		this.searchString = searchString;
	}
}

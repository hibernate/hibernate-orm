/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.hbm2x.hbm2hbmxml.SetElementTest;

import java.util.SortedSet;
import java.util.TreeSet;

public class Search {
	private String searchString;
	private SortedSet<String> searchResults = new TreeSet<>();

	Search() {}

	public Search(String string) {
		searchString = string;
	}

	public SortedSet<String> getSearchResults() {
		return searchResults;
	}
	public void setSearchResults(SortedSet<String> searchResults) {
		this.searchResults = searchResults;
	}
	public String getSearchString() {
		return searchString;
	}
	public void setSearchString(String searchString) {
		this.searchString = searchString;
	}
}

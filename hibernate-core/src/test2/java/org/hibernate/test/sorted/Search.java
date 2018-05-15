/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: Search.java 7772 2005-08-05 23:03:46Z oneovthafew $
package org.hibernate.test.sorted;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

public class Search {
	private String searchString;
	private SortedSet searchResults = new TreeSet();
	private SortedMap<String,String> tokens = new TreeMap<>();

	Search() {}
	
	public Search(String string) {
		searchString = string;
	}

	public String getSearchString() {
		return searchString;
	}

	public void setSearchString(String searchString) {
		this.searchString = searchString;
	}

	public SortedSet getSearchResults() {
		return searchResults;
	}

	public void setSearchResults(SortedSet searchResults) {
		this.searchResults = searchResults;
	}

	public SortedMap<String, String> getTokens() {
		return tokens;
	}

	public void setTokens(SortedMap<String, String> tokens) {
		this.tokens = tokens;
	}
}

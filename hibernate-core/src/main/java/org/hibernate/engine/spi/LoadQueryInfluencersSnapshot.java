/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.spi;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;

import org.hibernate.Filter;
import org.hibernate.internal.FilterImpl;

/**
 * A snapshot of {@link LoadQueryInfluencers} to apply to lazy loading outside of a transaction.
 *
 * @author Christian Beikov
 */
public class LoadQueryInfluencersSnapshot implements Serializable {

	final HashSet<String> enabledFetchProfileNames;
	final HashMap<String, FilterImpl> enabledFilters;

	LoadQueryInfluencersSnapshot(HashSet<String> enabledFetchProfileNames, HashMap<String, FilterImpl> enabledFilters) {
		this.enabledFetchProfileNames = enabledFetchProfileNames;
		this.enabledFilters = enabledFilters;
	}
}

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.convert.results.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.loader.PropertyPath;
import org.hibernate.sql.convert.results.spi.Fetch;
import org.hibernate.sql.convert.results.spi.FetchParent;
import org.hibernate.sql.exec.results.process.spi.InitializerCollector;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractFetchParent implements FetchParent {
	private final PropertyPath propertyPath;
	private final String tableGroupUid;

	private List<Fetch> fetches;

	public AbstractFetchParent(PropertyPath propertyPath, String tableGroupUid) {
		this.propertyPath = propertyPath;
		this.tableGroupUid = tableGroupUid;
	}

	@Override
	public PropertyPath getPropertyPath() {
		return propertyPath;
	}

	@Override
	public String getTableGroupUniqueIdentifier() {
		return tableGroupUid;
	}

	@Override
	public void addFetch(Fetch fetch) {
		if ( fetches == null ) {
			fetches = new ArrayList<>();
		}

		fetches.add( fetch );
	}

	@Override
	public List<Fetch> getFetches() {
		return fetches == null ? Collections.emptyList() : Collections.unmodifiableList( fetches );
	}

	protected void addFetchInitializers(InitializerCollector collector) {
		if ( fetches == null ) {
			return;
		}

		for ( Fetch fetch : fetches ) {
			fetch.registerInitializers( collector );
		}
	}
}

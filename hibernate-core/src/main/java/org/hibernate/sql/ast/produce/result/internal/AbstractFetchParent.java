/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.result.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.query.spi.NavigablePath;
import org.hibernate.sql.ast.consume.results.spi.InitializerCollector;
import org.hibernate.sql.ast.produce.result.spi.Fetch;
import org.hibernate.sql.ast.produce.result.spi.FetchParent;
import org.hibernate.sql.ast.tree.spi.expression.domain.ColumnReferenceSource;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractFetchParent implements FetchParent {
	private final NavigablePath navigablePath;
	private final ColumnReferenceSource columnReferenceSource;

	private List<Fetch> fetches;

	public AbstractFetchParent(NavigablePath navigablePath, ColumnReferenceSource columnReferenceSource) {
		this.navigablePath = navigablePath;
		this.columnReferenceSource = columnReferenceSource;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public String getTableGroupUniqueIdentifier() {
		return columnReferenceSource.getUniqueIdentifier();
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
		final List<Fetch> base = fetches == null ? Collections.emptyList() : Collections.unmodifiableList( fetches );
		return new ArrayList<>( base );
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

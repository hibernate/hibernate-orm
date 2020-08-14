/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.internal;

import java.util.function.Consumer;

import org.hibernate.query.NavigablePath;
import org.hibernate.query.named.FetchMemento;
import org.hibernate.query.results.FetchBuilder;

/**
 * @author Steve Ebersole
 */
public class FetchMementoJpa implements FetchMemento {

	private final NavigablePath fetchPath;
	private final String attributePath;

	public FetchMementoJpa(NavigablePath fetchPath, String attributePath) {
		this.fetchPath = fetchPath;
		this.attributePath = attributePath;
	}

	@Override
	public FetchBuilder resolve(
			Parent parent,
			Consumer<String> querySpaceConsumer, ResultSetMappingResolutionContext context) {
		final String[] names = attributePath.split( "\\." );

		NavigablePath fetchPath = parent.getNavigablePath();
		//noinspection ForLoopReplaceableByForEach
		for ( int i = 0; i < names.length; i++ ) {
			fetchPath = fetchPath.append( names[ i ] );
		}

		return new FetchBuilderJpa( fetchPath, attributePath );
	}

	@Override
	public NavigablePath getNavigablePath() {
		return fetchPath;
	}

}

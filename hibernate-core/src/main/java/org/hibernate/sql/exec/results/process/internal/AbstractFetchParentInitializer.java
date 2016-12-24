/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.exec.results.process.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.sql.exec.results.process.spi.FetchInitializer;
import org.hibernate.sql.exec.results.process.spi.Initializer;
import org.hibernate.sql.exec.results.process.spi.InitializerParent;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractFetchParentInitializer implements Initializer, InitializerParent {
	private final InitializerParent initializerParent;
	private List<FetchInitializer> fetchInitializers;

	public AbstractFetchParentInitializer(InitializerParent initializerParent) {
		this.initializerParent = initializerParent;
	}

	public InitializerParent getInitializerParent() {
		return initializerParent;
	}

	@Override
	public List<FetchInitializer> getChildFetchInitializers() {
		return fetchInitializers == null ? Collections.emptyList() : Collections.unmodifiableList( fetchInitializers );
	}

	@Override
	public void addChildFetchInitializer(FetchInitializer fetchInitializer) {
		if ( fetchInitializers == null ) {
			fetchInitializers = new ArrayList<>();
		}
		fetchInitializers.add( fetchInitializer );
	}
}

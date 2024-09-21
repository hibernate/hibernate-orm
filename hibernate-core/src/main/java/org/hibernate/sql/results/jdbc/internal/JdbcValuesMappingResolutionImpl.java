/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.jdbc.internal;

import java.util.ArrayList;

import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.Initializer;
import org.hibernate.sql.results.internal.InitializersList;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMappingResolution;

public class JdbcValuesMappingResolutionImpl implements JdbcValuesMappingResolution {

	private final DomainResultAssembler<?>[] domainResultAssemblers;
	private final Initializer<?>[] resultInitializers;
	private final boolean hasCollectionInitializers;
	private final InitializersList initializersList;

	public JdbcValuesMappingResolutionImpl(
			DomainResultAssembler<?>[] domainResultAssemblers,
			boolean hasCollectionInitializers,
			InitializersList initializersList) {
		this( domainResultAssemblers, getResultInitializers( domainResultAssemblers ), hasCollectionInitializers, initializersList );
	}

	private JdbcValuesMappingResolutionImpl(
			DomainResultAssembler<?>[] domainResultAssemblers,
			Initializer<?>[] resultInitializers,
			boolean hasCollectionInitializers,
			InitializersList initializersList) {
		this.domainResultAssemblers = domainResultAssemblers;
		this.resultInitializers = resultInitializers;
		this.hasCollectionInitializers = hasCollectionInitializers;
		this.initializersList = initializersList;
	}

	private static Initializer<?>[] getResultInitializers(DomainResultAssembler<?>[] resultAssemblers) {
		final ArrayList<Initializer<?>> initializers = new ArrayList<>( resultAssemblers.length );
		for ( DomainResultAssembler<?> resultAssembler : resultAssemblers ) {
			resultAssembler.forEachResultAssembler( (initializer, list) -> list.add( initializer ), initializers );
		}
		return initializers.toArray(Initializer.EMPTY_ARRAY);
	}

	@Override
	public DomainResultAssembler<?>[] getDomainResultAssemblers() {
		return domainResultAssemblers;
	}

	@Override
	public boolean hasCollectionInitializers() {
		return hasCollectionInitializers;
	}

	@Override
	public Initializer<?>[] getResultInitializers() {
		return resultInitializers;
	}

	@Override
	public Initializer<?>[] getInitializers() {
		return initializersList.getInitializers();
	}

	@Override
	public Initializer<?>[] getSortedForResolveInstance() {
		return initializersList.getSortedForResolveInstance();
	}

}

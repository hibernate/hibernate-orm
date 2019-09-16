/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal.domain;

import java.util.function.Consumer;

import org.hibernate.LockMode;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.FetchTiming;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.spi.AssemblerCreationState;
import org.hibernate.sql.results.spi.BiDirectionalFetch;
import org.hibernate.sql.results.spi.DomainResultAssembler;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.sql.results.spi.EntityResult;
import org.hibernate.sql.results.spi.Fetch;
import org.hibernate.sql.results.spi.FetchParent;
import org.hibernate.sql.results.spi.FetchParentAccess;
import org.hibernate.sql.results.spi.Fetchable;
import org.hibernate.sql.results.spi.Initializer;
import org.hibernate.sql.results.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.results.spi.RowProcessingState;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * @author Andrea Boriero
 */
public class RootBiDirectionalFetchImpl implements BiDirectionalFetch, Fetchable {
	private final NavigablePath navigablePath;
	private final FetchParent fetchParent;
	private final EntityResult referencedRoot;

	public RootBiDirectionalFetchImpl(
			NavigablePath navigablePath,
			FetchParent fetchParent,
			EntityResult referencedRoot) {
		this.fetchParent = fetchParent;
		this.referencedRoot = referencedRoot;
		this.navigablePath = navigablePath;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public NavigablePath getReferencedPath() {
		return referencedRoot.getNavigablePath();
	}

	@Override
	public FetchParent getFetchParent() {
		return fetchParent;
	}

	@Override
	public Fetchable getFetchedMapping() {
		return null;
	}

	@Override
	public boolean isNullable() {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public DomainResultAssembler createAssembler(
			FetchParentAccess parentAccess,
			Consumer<Initializer> collector,
			AssemblerCreationState creationState) {
		return new CircularFetchAssembler(
				getReferencedPath(),
				referencedRoot.getResultJavaTypeDescriptor()
		);
	}

	@Override
	public String getFetchableName() {
		// An entity itself is not fetchable
		return null;
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return referencedRoot.getResultJavaTypeDescriptor();
	}

	@Override
	public FetchStrategy getMappedFetchStrategy() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Fetch generateFetch(
			FetchParent fetchParent,
			FetchTiming fetchTiming,
			boolean selected,
			LockMode lockMode,
			String resultVariable,
			DomainResultCreationState creationState) {
		throw new UnsupportedOperationException();
	}

	private static class CircularFetchAssembler implements DomainResultAssembler {
		private final NavigablePath circularPath;
		private final JavaTypeDescriptor javaTypeDescriptor;

		public CircularFetchAssembler(
				NavigablePath circularPath,
				JavaTypeDescriptor javaTypeDescriptor) {
			this.circularPath = circularPath;
			this.javaTypeDescriptor = javaTypeDescriptor;
		}

		@Override
		public Object assemble(RowProcessingState rowProcessingState, JdbcValuesSourceProcessingOptions options) {
			Initializer initializer = rowProcessingState.resolveInitializer( circularPath );
			if ( initializer.getInitializedInstance() == null ) {
				initializer.resolveKey( rowProcessingState );
				initializer.resolveInstance( rowProcessingState );
				initializer.initializeInstance( rowProcessingState );
			}
			return initializer.getInitializedInstance();
		}

		@Override
		public JavaTypeDescriptor getAssembledJavaTypeDescriptor() {
			return javaTypeDescriptor;
		}
	}

}

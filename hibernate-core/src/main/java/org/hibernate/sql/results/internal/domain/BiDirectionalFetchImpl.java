/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal.domain;

import java.util.function.Consumer;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.spi.AssemblerCreationState;
import org.hibernate.sql.results.spi.BiDirectionalFetch;
import org.hibernate.sql.results.spi.DomainResultAssembler;
import org.hibernate.sql.results.spi.Fetch;
import org.hibernate.sql.results.spi.FetchParent;
import org.hibernate.sql.results.spi.FetchParentAccess;
import org.hibernate.sql.results.spi.Fetchable;
import org.hibernate.sql.results.spi.Initializer;
import org.hibernate.sql.results.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.results.spi.RowProcessingState;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * Implementation of BiDirectionalFetch for the `oa` case - the bi-dir fetch
 * refers to another fetch (`a`)
 *
 * @author Steve Ebersole
 */
public class BiDirectionalFetchImpl implements BiDirectionalFetch {
	private final NavigablePath navigablePath;
	private final FetchParent fetchParent;
	private final Fetch referencedFetch;

	/**
	 * Create the bi-dir fetch
	 *
	 * @param navigablePath `Person(p).address.owner.address`
	 * @param fetchParent The parent for the `oa` fetch is `o`
	 * @param referencedFetch `RootBiDirectionalFetchImpl(a)` (because `a` is itself also a bi-dir fetch referring to the `p root)
	 */
	public BiDirectionalFetchImpl(
			NavigablePath navigablePath,
			FetchParent fetchParent,
			Fetch referencedFetch) {
		this.navigablePath = navigablePath;
		this.fetchParent = fetchParent;
		this.referencedFetch = referencedFetch;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public NavigablePath getReferencedPath() {
		return referencedFetch.getNavigablePath();
	}

	@Override
	public FetchParent getFetchParent() {
		return fetchParent;
	}

	@Override
	public Fetchable getFetchedMapping() {
		return referencedFetch.getFetchedMapping();
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
				referencedFetch.getFetchedMapping().getJavaTypeDescriptor()
		);
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
			return rowProcessingState.resolveInitializer( circularPath ).getInitializedInstance();
		}

		@Override
		public JavaTypeDescriptor getAssembledJavaTypeDescriptor() {
			return javaTypeDescriptor;
		}
	}

}

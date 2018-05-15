/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal;

import java.util.function.Consumer;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.spi.AssemblerCreationContext;
import org.hibernate.sql.results.spi.AssemblerCreationState;
import org.hibernate.sql.results.spi.BiDirectionalFetch;
import org.hibernate.sql.results.spi.DomainResultAssembler;
import org.hibernate.sql.results.spi.Fetch;
import org.hibernate.sql.results.spi.FetchParent;
import org.hibernate.sql.results.spi.FetchParentAccess;
import org.hibernate.sql.results.spi.Initializer;
import org.hibernate.sql.results.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.results.spi.RowProcessingState;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class BiDirectionalFetchImpl implements BiDirectionalFetch {
	private final FetchParent referencedFetchParent;
	private final NavigablePath navigablePath;

	public BiDirectionalFetchImpl(FetchParent referencedFetch, NavigablePath navigablePath) {
		if ( referencedFetch instanceof BiDirectionalFetch ) {
			referencedFetch = ( (BiDirectionalFetch) referencedFetch ).getFetchParent();
		}
		this.referencedFetchParent = referencedFetch;
		this.navigablePath = navigablePath;
	}

	@Override
	public FetchParent getFetchParent() {
		return referencedFetchParent;
	}

	@Override
	public Navigable getFetchedNavigable() {
		return referencedFetchParent.getNavigableContainer();
	}

	@Override
	public String getFetchedNavigableName(){
		return navigablePath.getLocalName();
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public boolean isNullable() {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	Fetch getFetch() {
		return this;
	}

	@Override
	public DomainResultAssembler createAssembler(
			FetchParentAccess parentAccess,
			Consumer<Initializer> collector,
			AssemblerCreationContext creationContext,
			AssemblerCreationState creationState) {
		return new CircularFetchAssembler(
				referencedFetchParent.getNavigablePath(),
				getJavaTypeDescriptor()
		);
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return referencedFetchParent.getNavigableContainer().getJavaTypeDescriptor();
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
		public JavaTypeDescriptor getJavaTypeDescriptor() {
			return javaTypeDescriptor;
		}
	}

}

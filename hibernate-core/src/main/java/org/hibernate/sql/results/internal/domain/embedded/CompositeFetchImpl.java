/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal.domain.embedded;

import java.util.function.Consumer;

import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.model.domain.internal.SingularPersistentAttributeEmbedded;
import org.hibernate.metamodel.model.domain.spi.EmbeddedValuedNavigable;
import org.hibernate.sql.results.internal.domain.AbstractFetchParent;
import org.hibernate.sql.results.spi.AssemblerCreationContext;
import org.hibernate.sql.results.spi.AssemblerCreationState;
import org.hibernate.sql.results.spi.CompositeFetch;
import org.hibernate.sql.results.spi.DomainResultAssembler;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.sql.results.spi.FetchParent;
import org.hibernate.sql.results.spi.FetchParentAccess;
import org.hibernate.sql.results.spi.Initializer;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class CompositeFetchImpl extends AbstractFetchParent implements CompositeFetch {
	private final FetchParent fetchParent;
	private final SingularPersistentAttributeEmbedded fetchedNavigable;
	private final FetchTiming fetchTiming;


	public CompositeFetchImpl(
			FetchParent fetchParent,
			SingularPersistentAttributeEmbedded fetchedNavigable,
			FetchTiming fetchTiming,
			DomainResultCreationState creationState) {
		super(
				fetchedNavigable,
				fetchParent.getNavigablePath().append( fetchedNavigable.getNavigableName() )
		);
		this.fetchParent = fetchParent;
		this.fetchedNavigable = fetchedNavigable;
		this.fetchTiming = fetchTiming;

		afterInitialize( creationState );
	}


	@Override
	public FetchParent getFetchParent() {
		return fetchParent;
	}

	@Override
	public EmbeddedValuedNavigable getCompositeNavigableDescriptor() {
		return getFetchedNavigable();
	}

	@Override
	public SingularPersistentAttributeEmbedded getFetchedNavigable() {
		return fetchedNavigable;
	}

	@Override
	public boolean isNullable() {
		return fetchedNavigable.isOptional();
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return getFetchedNavigable().getJavaTypeDescriptor();
	}


	@Override
	public DomainResultAssembler createAssembler(
			FetchParentAccess parentAccess,
			Consumer<Initializer> collector,
			AssemblerCreationContext context,
			AssemblerCreationState creationState) {
		final CompositeFetchInitializerImpl initializer = new CompositeFetchInitializerImpl(
				parentAccess,
				this,
				collector,
				context,
				creationState
		);

		collector.accept( initializer );

		return new CompositeAssembler( initializer );
	}
}

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.internal.select;

import org.hibernate.engine.FetchStrategy;
import org.hibernate.metamodel.model.domain.internal.SingularPersistentAttributeEmbedded;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.sql.NotYetImplementedException;
import org.hibernate.sql.exec.results.internal.CompositeReferenceInitializerImpl;
import org.hibernate.sql.exec.results.spi.CompositeReferenceInitializer;
import org.hibernate.sql.exec.results.spi.Initializer;
import org.hibernate.sql.exec.results.spi.InitializerCollector;
import org.hibernate.sql.ast.tree.spi.select.FetchCompositeAttribute;
import org.hibernate.sql.ast.tree.spi.select.FetchParent;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableContainerReference;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;

/**
 * @author Steve Ebersole
 */
public class FetchCompositeAttributeImpl extends AbstractFetchParent implements FetchCompositeAttribute {
	private final FetchParent fetchParent;
	private final NavigableReference fetchedNavigableReference;
	private final FetchStrategy fetchStrategy;

	private final CompositeReferenceInitializer initializer;

	public FetchCompositeAttributeImpl(
			FetchParent fetchParent,
			NavigableContainerReference fetchedNavigableReference,
			FetchStrategy fetchStrategy) {
		super(
				fetchedNavigableReference,
				fetchParent.getNavigablePath().append( fetchedNavigableReference.getNavigable().getNavigableName() )
		);
		this.fetchParent = fetchParent;
		this.fetchedNavigableReference = fetchedNavigableReference;
		this.fetchStrategy = fetchStrategy;

		this.initializer = new CompositeReferenceInitializerImpl(
				fetchParent.getInitializerParentForFetchInitializers()
		);
	}

	public NavigableReference getFetchedNavigableReference() {
		return fetchedNavigableReference;
	}

	@Override
	public FetchParent getFetchParent() {
		return fetchParent;
	}

	@Override
	public Navigable getFetchedNavigable() {
		return getFetchedNavigableReference().getNavigable();
	}


	@Override
	public SingularPersistentAttributeEmbedded getFetchedAttributeDescriptor() {
		return (SingularPersistentAttributeEmbedded) fetchedNavigableReference.getNavigable();
	}

	@Override
	public FetchStrategy getFetchStrategy() {
		return fetchStrategy;
	}

	@Override
	public boolean isNullable() {
		throw new NotYetImplementedException(  );
	}

	@Override
	public NavigableContainerReference getNavigableContainerReference() {
		return getFetchedNavigableReference().getNavigableContainerReference();
	}

	@Override
	public CompositeReferenceInitializer getInitializerParentForFetchInitializers() {
		return initializer;
	}

	@Override
	public void registerInitializers(InitializerCollector collector) {
		collector.addInitializer( initializer );
	}

	@Override
	public Initializer getInitializer() {
		throw new NotYetImplementedException(  );
	}
}

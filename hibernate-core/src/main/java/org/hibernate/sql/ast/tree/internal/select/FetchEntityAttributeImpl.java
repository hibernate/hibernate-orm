/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.internal.select;

import org.hibernate.engine.FetchStrategy;
import org.hibernate.metamodel.model.domain.internal.SingularPersistentAttributeEntity;
import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.NotYetImplementedException;
import org.hibernate.sql.ast.tree.spi.expression.domain.EntityReference;
import org.hibernate.sql.ast.tree.spi.select.EntityIdentifierReference;
import org.hibernate.sql.ast.tree.spi.select.FetchEntityAttribute;
import org.hibernate.sql.ast.tree.spi.select.FetchParent;
import org.hibernate.sql.ast.tree.spi.select.QueryResultCreationContext;
import org.hibernate.sql.ast.tree.spi.select.SqlSelectionResolver;
import org.hibernate.sql.exec.results.internal.EntityFetchInitializerImpl;
import org.hibernate.sql.exec.results.spi.EntityReferenceInitializer;
import org.hibernate.sql.exec.results.spi.InitializerCollector;
import org.hibernate.sql.exec.results.spi.InitializerParent;

/**
 * @author Steve Ebersole
 */
public class FetchEntityAttributeImpl extends AbstractFetchParent implements FetchEntityAttribute {
	private final FetchParent fetchParent;
	private final FetchStrategy fetchStrategy;

	private final EntityFetchInitializerImpl initializer;

	public FetchEntityAttributeImpl(
			FetchParent fetchParent,
			EntityReference entityReference,
			NavigablePath navigablePath,
			FetchStrategy fetchStrategy,
			SqlSelectionResolver sqlSelectionResolver,
			QueryResultCreationContext creationContext) {
		super( entityReference, navigablePath );
		this.fetchParent = fetchParent;
		this.fetchStrategy = fetchStrategy;

		this.initializer = new EntityFetchInitializerImpl(
				fetchParent.getInitializerParentForFetchInitializers(),
				this,
				null,
				false
		);
	}

	@Override
	public FetchParent getFetchParent() {
		return fetchParent;
	}

	@Override
	public Navigable getFetchedNavigable() {
		return getNavigableContainerReference().getNavigable();
	}

	@Override
	public SingularPersistentAttributeEntity getFetchedAttributeDescriptor() {
		return (SingularPersistentAttributeEntity) getFetchedNavigable();
	}

	@Override
	public FetchStrategy getFetchStrategy() {
		return fetchStrategy;
	}

	@Override
	public boolean isNullable() {
		throw new NotYetImplementedException(  );
	}

//	@Override
//	public ResolvedFetch resolve(
//			ResolvedFetchParent resolvedFetchParent,
//			Map<AttributeDescriptor, SqlSelectionGroup> sqlSelectionGroupMap,
//			boolean shallow) {
//		return new ResolvedFetchEntityImpl(
//				this,
//				resolvedFetchParent,
//				fetchStrategy,
//				sqlSelectionGroupMap,
//				shallow
//		);
//	}

	@Override
	public EntityDescriptor getEntityDescriptor() {
		return getFetchedAttributeDescriptor().getAssociatedEntityDescriptor();
	}

	@Override
	public EntityIdentifierReference getIdentifierReference() {
		throw new NotYetImplementedException(  );
	}

	@Override
	public void registerInitializers(InitializerCollector collector) {
		collector.addInitializer( getInitializer() );
		addFetchInitializers( collector );
	}

	@Override
	public EntityReferenceInitializer getInitializer() {
		return initializer;
	}

	@Override
	public InitializerParent getInitializerParentForFetchInitializers() {
		return initializer;
	}
}

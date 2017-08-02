/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.exec.results.internal;

import org.hibernate.engine.FetchStrategy;
import org.hibernate.metamodel.model.domain.internal.SingularPersistentAttributeEntity;
import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityValuedNavigable;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.NotYetImplementedException;
import org.hibernate.sql.exec.results.spi.EntityReference;
import org.hibernate.sql.exec.results.spi.FetchParentAccess;
import org.hibernate.sql.exec.results.spi.InitializerEntity;
import org.hibernate.sql.exec.results.spi.FetchEntityAttribute;
import org.hibernate.sql.exec.results.spi.FetchParent;
import org.hibernate.sql.exec.results.spi.InitializerCollector;
import org.hibernate.sql.exec.results.spi.InitializerHelper;
import org.hibernate.sql.exec.results.spi.InitializerParent;
import org.hibernate.sql.exec.results.spi.QueryResultCreationContext;

/**
 * @author Steve Ebersole
 */
public class FetchEntityAttributeImpl extends AbstractFetchParent implements FetchEntityAttribute {
	private final FetchParent fetchParent;
	private final FetchParentAccess fetchParentAccess;
	private final FetchStrategy fetchStrategy;

	private final EntityFetchInitializerImpl initializer;

	public FetchEntityAttributeImpl(
			FetchParent fetchParent,
			FetchParentAccess fetchParentAccess,
			EntityValuedNavigable fetchedNavigable,
			NavigablePath navigablePath,
			FetchStrategy fetchStrategy,
			QueryResultCreationContext creationContext) {
		super( fetchedNavigable, navigablePath );
		this.fetchParent = fetchParent;
		this.fetchParentAccess = fetchParentAccess;
		this.fetchStrategy = fetchStrategy;

		this.initializer = new EntityFetchInitializerImpl(
				fetchParentAccess,
				this,
				InitializerHelper.resolveSqlSelectionMappings(
						this,
						creationContext
				),
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
	public void registerInitializers(FetchParentAccess parentAccess, InitializerCollector collector) {
		collector.addInitializer( initializer );
		registerFetchInitializers( initializer, collector );
	}

	@Override
	public void registerInitializers(InitializerCollector collector) {
		collector.addInitializer( initializer );
		registerFetchInitializers( getInitializer(), collector );
	}

	@Override
	public InitializerEntity getInitializer() {
		return initializer;
	}

	@Override
	public InitializerParent getInitializerParentForFetchInitializers() {
		return initializer;
	}
}

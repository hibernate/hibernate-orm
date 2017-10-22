/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import org.hibernate.boot.model.domain.ManagedTypeMapping;
import org.hibernate.engine.FetchStrategy;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.model.domain.spi.ManagedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.metamodel.model.domain.spi.NavigableVisitationStrategy;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.metamodel.model.domain.spi.PluralPersistentAttribute;
import org.hibernate.metamodel.model.relational.spi.ForeignKey;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.sql.ast.produce.spi.ColumnReferenceQualifier;
import org.hibernate.sql.results.spi.Fetch;
import org.hibernate.sql.results.spi.FetchParent;
import org.hibernate.sql.results.spi.QueryResultCreationContext;
import org.hibernate.sql.results.spi.SqlSelectionGroup;
import org.hibernate.sql.results.spi.SqlSelectionGroupResolutionContext;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class PluralPersistentAttributeImpl implements PluralPersistentAttribute {
	private final PersistentCollectionDescriptor collectionDescriptor;

	public PluralPersistentAttributeImpl(
			PersistentCollectionDescriptor collectionDescriptor,
			Property bootProperty,
			RuntimeModelCreationContext creationContext) {

		final Collection bootCollectionDescriptor = (Collection) bootProperty.getValue();

		this.collectionDescriptor = collectionDescriptor;

		creationContext.registerCollectionDescriptor( collectionDescriptor, bootCollectionDescriptor );
	}

	@Override
	public PersistentCollectionDescriptor getPersistentCollectionDescriptor() {
		return collectionDescriptor;
	}

	@Override
	public Class getJavaType() {
		return getJavaTypeDescriptor().getJavaType();
	}

	@Override
	public BindableType getBindableType() {
		return BindableType.PLURAL_ATTRIBUTE;
	}

	@Override
	public ManagedTypeDescriptor getContainer() {
		return null;
	}

	@Override
	public PropertyAccess getPropertyAccess() {
		return null;
	}

	@Override
	public boolean isIncludedInOptimisticLocking() {
		return false;
	}

	@Override
	public PersistentAttributeType getPersistentAttributeType() {
		return null;
	}

	@Override
	public boolean isAssociation() {
		return false;
	}

	@Override
	public boolean isCollection() {
		return false;
	}

	@Override
	public Class getBindableJavaType() {
		return null;
	}

	@Override
	public boolean isNullable() {
		return false;
	}

	@Override
	public boolean isInsertable() {
		return false;
	}

	@Override
	public boolean isUpdatable() {
		return false;
	}

	@Override
	public boolean isIncludedInDirtyChecking() {
		return false;
	}

	@Override
	public MutabilityPlan getMutabilityPlan() {
		return null;
	}

	@Override
	public Navigable findNavigable(String navigableName) {
		return getPersistentCollectionDescriptor().findNavigable( navigableName );
	}

	@Override
	public void visitNavigables(NavigableVisitationStrategy visitor) {
		getPersistentCollectionDescriptor().visitNavigables( visitor );
	}

	@Override
	public Fetch generateFetch(
			FetchParent fetchParent,
			ColumnReferenceQualifier qualifier,
			FetchStrategy fetchStrategy,
			String resultVariable,
			QueryResultCreationContext creationContext) {
		return null;
	}

	@Override
	public FetchStrategy getMappedFetchStrategy() {
		return null;
	}

	@Override
	public ManagedTypeDescriptor getFetchedManagedType() {
		return null;
	}

	@Override
	public ForeignKey.ColumnMappings getJoinColumnMappings() {
		return null;
	}

	@Override
	public NavigableRole getNavigableRole() {
		return null;
	}

	@Override
	public SqlSelectionGroup resolveSqlSelectionGroup(
			ColumnReferenceQualifier qualifier, SqlSelectionGroupResolutionContext resolutionContext) {
		return null;
	}

	@Override
	public String asLoggableText() {
		return null;
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return null;
	}
}

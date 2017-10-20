/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import java.io.Serializable;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.boot.model.domain.ManagedTypeMapping;
import org.hibernate.cache.spi.access.CollectionDataAccess;
import org.hibernate.cache.spi.entry.CacheEntryStructure;
import org.hibernate.collection.spi.CollectionClassification;
import org.hibernate.collection.spi.PersistentCollectionRepresentation;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.spi.CollectionLoader;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.model.domain.spi.AbstractPersistentCollectionDescriptor;
import org.hibernate.metamodel.model.domain.spi.CollectionElement;
import org.hibernate.metamodel.model.domain.spi.CollectionIdentifier;
import org.hibernate.metamodel.model.domain.spi.CollectionIndex;
import org.hibernate.metamodel.model.domain.spi.CollectionKey;
import org.hibernate.metamodel.model.domain.spi.ManagedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.PluralPersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.TableReferenceJoinCollector;
import org.hibernate.metamodel.model.relational.spi.Table;
import org.hibernate.sql.ast.JoinType;
import org.hibernate.sql.ast.produce.spi.ColumnReferenceQualifier;
import org.hibernate.sql.ast.produce.spi.SqlAliasBase;
import org.hibernate.sql.ast.produce.spi.TableGroupContext;
import org.hibernate.sql.results.spi.SqlSelectionGroup;
import org.hibernate.sql.results.spi.SqlSelectionGroupResolutionContext;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * Hibernate's standard PersistentCollectionDescriptor implementor
 * for Lists
 *
 * @author Steve Ebersole
 */
public class PersistentListDescriptorImpl extends AbstractPersistentCollectionDescriptor {
	public PersistentListDescriptorImpl(
			Property bootProperty,
			ManagedTypeDescriptor runtimeContainer,
			PersistentCollectionRepresentation representation,
			RuntimeModelCreationContext context) {
		super( bootProperty, runtimeContainer, representation, CollectionClassification.LIST, context );
	}

	@Override
	protected Table resolveCollectionTable(
			Collection collectionBinding,
			RuntimeModelCreationContext creationContext) {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public ManagedTypeDescriptor getContainer() {
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
	public PluralPersistentAttribute getDescribedAttribute() {
		return null;
	}

	@Override
	public CollectionKey getForeignKeyDescriptor() {
		return null;
	}

	@Override
	public CollectionIdentifier getIdDescriptor() {
		return null;
	}

	@Override
	public CollectionElement getElementDescriptor() {
		return null;
	}

	@Override
	public CollectionIndex getIndexDescriptor() {
		return null;
	}

	@Override
	public PersistentCollectionRepresentation getTuplizer() {
		return null;
	}

	@Override
	public CollectionLoader getLoader() {
		return null;
	}

	@Override
	public Table getSeparateCollectionTable() {
		return null;
	}

	@Override
	public boolean isInverse() {
		return false;
	}

	@Override
	public boolean hasOrphanDelete() {
		return false;
	}

	@Override
	public boolean isOneToMany() {
		return false;
	}

	@Override
	public boolean isExtraLazy() {
		return false;
	}

	@Override
	public boolean isDirty(Object old, Object value, SharedSessionContractImplementor session) {
		return false;
	}

	@Override
	public int getSize(Serializable loadedKey, SharedSessionContractImplementor session) {
		return 0;
	}

	@Override
	public Boolean indexExists(
			Serializable loadedKey, Object index, SharedSessionContractImplementor session) {
		return null;
	}

	@Override
	public Boolean elementExists(
			Serializable loadedKey, Object element, SharedSessionContractImplementor session) {
		return null;
	}

	@Override
	public Object getElementByIndex(
			Serializable loadedKey, Object index, SharedSessionContractImplementor session, Object owner) {
		return null;
	}

	@Override
	public CacheEntryStructure getCacheEntryStructure() {
		return null;
	}

	@Override
	public CollectionDataAccess getCacheAccess() {
		return null;
	}

	@Override
	public String getMappedByProperty() {
		return null;
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return null;
	}

	@Override
	public boolean isAffectedByEnabledFilters(SharedSessionContractImplementor session) {
		return false;
	}

	@Override
	public void applyTableReferenceJoins(
			ColumnReferenceQualifier lhs,
			JoinType joinType,
			SqlAliasBase sqlAliasBase,
			TableReferenceJoinCollector joinCollector,
			TableGroupContext tableGroupContext) {

	}
}

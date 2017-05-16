/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.embedded.internal;

import java.util.Collections;
import java.util.List;

import org.hibernate.boot.model.domain.AttributeMapping;
import org.hibernate.boot.model.domain.EmbeddedValueMapping;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.mapping.Component;
import org.hibernate.persister.common.NavigableRole;
import org.hibernate.persister.common.internal.PersisterHelper;
import org.hibernate.persister.common.spi.AbstractManagedType;
import org.hibernate.persister.common.spi.Column;
import org.hibernate.persister.common.spi.ManagedTypeImplementor;
import org.hibernate.persister.common.spi.NavigableVisitationStrategy;
import org.hibernate.persister.common.spi.PersistentAttribute;
import org.hibernate.persister.embedded.spi.EmbeddedContainer;
import org.hibernate.persister.embedded.spi.EmbeddedPersister;
import org.hibernate.persister.spi.PersisterCreationContext;
import org.hibernate.sql.ast.tree.spi.from.TableGroup;
import org.hibernate.sql.ast.tree.spi.from.TableSpace;
import org.hibernate.sql.ast.produce.spi.FromClauseIndex;
import org.hibernate.sql.ast.produce.spi.SqlAliasBaseManager;
import org.hibernate.sql.ast.produce.result.spi.Fetch;
import org.hibernate.sql.ast.produce.result.spi.FetchParent;
import org.hibernate.sql.ast.produce.result.spi.QueryResult;
import org.hibernate.sql.ast.produce.result.spi.QueryResultCreationContext;
import org.hibernate.type.descriptor.java.internal.EmbeddableJavaDescriptorImpl;
import org.hibernate.type.descriptor.java.spi.EmbeddableJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptorRegistry;
import org.hibernate.type.internal.EmbeddedTypeImpl;
import org.hibernate.type.spi.EmbeddedType;

/**
 * @author Steve Ebersole
 */
public class EmbeddedPersisterImpl<T> extends AbstractManagedType<T> implements EmbeddedPersister<T> {
	private final EmbeddedContainer source;
	private final NavigableRole navigableRole;
	private final EmbeddedType ormType;

	public EmbeddedPersisterImpl(
			Component bootMapping,
			EmbeddedContainer source,
			String localName,
			PersisterCreationContext creationContext) {
		super( resolveJtd( creationContext, bootMapping ) );
		this.source = source;
		this.navigableRole = source.getNavigableRole().append( localName );
		this.ormType = new EmbeddedTypeImpl( null, navigableRole, getJavaTypeDescriptor() );

		setTypeConfiguration( creationContext.getTypeConfiguration() );
	}

	@SuppressWarnings("unchecked")
	private static <T> EmbeddableJavaDescriptor<T> resolveJtd(PersisterCreationContext creationContext, Component embeddedMapping) {
		JavaTypeDescriptorRegistry jtdr = creationContext.getTypeConfiguration().getJavaTypeDescriptorRegistry();
		EmbeddableJavaDescriptor jtd = (EmbeddableJavaDescriptor) jtdr.getDescriptor( embeddedMapping.getType().getName() );
		if ( jtd == null ) {
			jtd = new EmbeddableJavaDescriptorImpl(
					embeddedMapping.getType().getName(),
					embeddedMapping.getType().getReturnedClass(),
					null
			);
			jtdr.addDescriptor( jtd );
		}
		return jtd;
	}

	@Override
	public void finishInstantiation(
			EmbeddedValueMapping embeddedValueMapping,
			PersisterCreationContext creationContext) {
		bindAttributes( embeddedValueMapping, creationContext );
	}

	@Override
	public void completeInitialization(
			EmbeddedValueMapping embeddedValueMapping,
			PersisterCreationContext creationContext) {
		// todo (6.0) : I think we will want some form of "after all mappings have been 'initialized' (see #finishInstantiation)"
	}

	@Override
	@SuppressWarnings("unchecked")
	public EmbeddableJavaDescriptor<T> getJavaTypeDescriptor() {
		return (EmbeddableJavaDescriptor<T>) super.getJavaTypeDescriptor();
	}

	@Override
	public String getNavigableName() {
		return navigableRole.getNavigableName();
	}

	@Override
	public String getRolePrefix() {
		return navigableRole.getFullPath();
	}

	@Override
	public EmbeddedPersister getEmbeddedPersister() {
		return this;
	}

	@Override
	public List<Column> collectColumns() {
		throw new NotYetImplementedException(  );
	}

	@Override
	public EmbeddedContainer<?> getContainer() {
		return source;
	}

	@Override
	public NavigableRole getNavigableRole() {
		return navigableRole;
	}

	@Override
	public EmbeddedType getOrmType() {
		return ormType;
	}

	@Override
	public void visitNavigable(NavigableVisitationStrategy visitor) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public TableGroup buildTableGroup(
			TableSpace tableSpace,
			SqlAliasBaseManager sqlAliasBaseManager,
			FromClauseIndex fromClauseIndex) {
		throw new NotYetImplementedException(  );
	}

	@Override
	public QueryResult generateReturn(
			QueryResultCreationContext returnResolutionContext, TableGroup tableGroup) {
		throw new NotYetImplementedException(  );
	}

	@Override
	public Fetch generateFetch(
			QueryResultCreationContext returnResolutionContext, TableGroup tableGroup, FetchParent fetchParent) {
		throw new NotYetImplementedException(  );
	}

	private void bindAttributes(EmbeddedValueMapping embeddableBinding, PersisterCreationContext creationContext) {
		for ( AttributeMapping attributeMapping : embeddableBinding.getAttributeMappings() ) {

			// todo (6.0) : Columns
			final List<Column> columns = Collections.emptyList();

			// todo (6.0) : new
			final PersistentAttribute persistentAttribute = PersisterHelper.INSTANCE.buildAttribute(
					creationContext,
					this,
					attributeMapping,
					columns
			);
			addAttribute( persistentAttribute );
		}
	}

	@Override
	public List<ManagedTypeImplementor<? extends T>> getSubclassTypes() {
		return Collections.emptyList();
	}

	@Override
	public void addSubclassType(ManagedTypeImplementor<? extends T> subclassType) {
		throw new UnsupportedOperationException( "Embeddable inheritance is not yet implemented" );
	}
}

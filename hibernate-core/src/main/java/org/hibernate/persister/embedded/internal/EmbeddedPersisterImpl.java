/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.embedded.internal;

import java.util.Collections;
import java.util.List;

import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Property;
import org.hibernate.persister.common.NavigableRole;
import org.hibernate.persister.common.internal.PersisterHelper;
import org.hibernate.persister.common.spi.AbstractManagedType;
import org.hibernate.persister.common.spi.Column;
import org.hibernate.persister.common.spi.NavigableVisitationStrategy;
import org.hibernate.persister.common.spi.PersistentAttribute;
import org.hibernate.persister.embedded.spi.EmbeddedContainer;
import org.hibernate.persister.embedded.spi.EmbeddedPersister;
import org.hibernate.persister.spi.PersisterCreationContext;
import org.hibernate.sql.ast.from.TableGroup;
import org.hibernate.sql.ast.from.TableSpace;
import org.hibernate.sql.convert.internal.FromClauseIndex;
import org.hibernate.sql.convert.internal.SqlAliasBaseManager;
import org.hibernate.sql.convert.results.spi.Fetch;
import org.hibernate.sql.convert.results.spi.FetchParent;
import org.hibernate.sql.convert.results.spi.Return;
import org.hibernate.sql.convert.results.spi.ReturnResolutionContext;
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
			Component componentBinding,
			EmbeddedContainer source,
			String localName,
			PersisterCreationContext creationContext) {
		super( resolveJtd( creationContext, componentBinding ) );
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
	@SuppressWarnings("unchecked")
	public EmbeddableJavaDescriptor<T> getJavaTypeDescriptor() {
		return (EmbeddableJavaDescriptor<T>) super.getJavaTypeDescriptor();
	}

	@Override
	public void afterInitialization(
			Component embeddableBinding,
			PersisterCreationContext creationContext) {
		bindAttributes( embeddableBinding, creationContext );
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
	public EmbeddedPersister getEmbeddablePersister() {
		return this;
	}

	@Override
	public List<Column> collectColumns() {
		throw new NotYetImplementedException(  );
	}

	@Override
	public EmbeddedContainer<?> getSource() {
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
	public Return generateReturn(
			ReturnResolutionContext returnResolutionContext, TableGroup tableGroup) {
		throw new NotYetImplementedException(  );
	}

	@Override
	public Fetch generateFetch(
			ReturnResolutionContext returnResolutionContext, TableGroup tableGroup, FetchParent fetchParent) {
		throw new NotYetImplementedException(  );
	}

	private void bindAttributes(Component embeddableBinding, PersisterCreationContext creationContext) {
		for ( Property property : embeddableBinding.getDeclaredProperties() ) {

			// todo : Columns
			final List<Column> columns = Collections.emptyList();

			final PersistentAttribute persistentAttribute = PersisterHelper.INSTANCE.buildAttribute(
					creationContext,
					this,
					property,
					columns
			);
			addAttribute( persistentAttribute );
		}
	}
}

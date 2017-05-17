/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.embedded.internal;

import java.util.Collections;
import java.util.List;

import org.hibernate.EntityMode;
import org.hibernate.boot.model.domain.EmbeddedMapping;
import org.hibernate.boot.model.domain.EmbeddedValueMapping;
import org.hibernate.boot.model.domain.PersistentAttributeMapping;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.internal.util.StringHelper;
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
import org.hibernate.sql.ast.produce.result.internal.QueryResultCompositeImpl;
import org.hibernate.sql.ast.produce.result.spi.QueryResult;
import org.hibernate.sql.ast.produce.result.spi.QueryResultCreationContext;
import org.hibernate.sql.ast.produce.result.spi.SqlSelectionResolver;
import org.hibernate.sql.ast.tree.spi.expression.domain.ColumnReferenceSource;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;
import org.hibernate.tuple.Tuplizer;
import org.hibernate.type.descriptor.java.internal.EmbeddableJavaDescriptorImpl;
import org.hibernate.type.descriptor.java.spi.EmbeddableJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptorRegistry;

/**
 * @author Steve Ebersole
 */
public class EmbeddedPersisterImpl<T> extends AbstractManagedType<T> implements EmbeddedPersister<T> {
	private final EmbeddedContainer container;
	private final NavigableRole navigableRole;

	public EmbeddedPersisterImpl(
			EmbeddedMapping embeddedMapping,
			EmbeddedContainer container,
			String localName,
			PersisterCreationContext creationContext) {
		super( resolveJtd( creationContext, embeddedMapping ) );
		this.container = container;
		this.navigableRole = container.getNavigableRole().append( localName );

		setTypeConfiguration( creationContext.getTypeConfiguration() );
	}

	@SuppressWarnings("unchecked")
	private static <T> EmbeddableJavaDescriptor<T> resolveJtd(PersisterCreationContext creationContext, EmbeddedMapping embeddedMapping) {
		JavaTypeDescriptorRegistry jtdr = creationContext.getTypeConfiguration().getJavaTypeDescriptorRegistry();
		EmbeddableJavaDescriptor<T> jtd = (EmbeddableJavaDescriptor<T>) jtdr.getDescriptor( embeddedMapping.getName() );
		if ( jtd == null ) {
			final Class<T> javaType;
			if ( StringHelper.isEmpty( embeddedMapping.getEmbeddableClassName() ) ) {
				javaType = null;
			}
			else {
				javaType = creationContext.getSessionFactory()
						.getServiceRegistry()
						.getService( ClassLoaderService.class )
						.classForName( embeddedMapping.getEmbeddableClassName() );
			}

			jtd = new EmbeddableJavaDescriptorImpl(
					embeddedMapping.getName(),
					javaType,
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

	@SuppressWarnings("AccessStaticViaInstance")
	private void bindAttributes(EmbeddedValueMapping embeddedValueMapping, PersisterCreationContext creationContext) {
		for ( PersistentAttributeMapping attributeMapping : embeddedValueMapping.getDeclaredPersistentAttributes() ) {
			final PersistentAttribute persistentAttribute = PersisterHelper.INSTANCE.buildAttribute(
					creationContext,
					this,
					attributeMapping
			);

			addAttribute( persistentAttribute );

			// todo (6.0) : need to capture the List<Column> per attribute.
			//		- not sure how
		}
	}

	@Override
	public void completeInitialization(
			EmbeddedValueMapping embeddedValueMapping,
			PersisterCreationContext creationContext) {
		// todo (6.0) : I think we will want some form of "after all mappings have been 'initialized' (see #finishInstantiation)"
	}

	@Override
	public EmbeddedContainer<?> getContainer() {
		return container;
	}

	@Override
	public EntityMode getEntityMode() {
		throw new NotYetImplementedException(  );
	}

	@Override
	public Tuplizer getTuplizer() {
		throw new NotYetImplementedException(  );
	}

	@Override
	public EmbeddedPersister<T> getEmbeddedPersister() {
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public EmbeddableJavaDescriptor<T> getJavaTypeDescriptor() {
		return (EmbeddableJavaDescriptor<T>) super.getJavaTypeDescriptor();
	}

	@Override
	public String getRolePrefix() {
		return navigableRole.getFullPath();
	}

	@Override
	public NavigableRole getNavigableRole() {
		return navigableRole;
	}

	@Override
	public void visitNavigable(NavigableVisitationStrategy visitor) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public QueryResult generateQueryResult(
			NavigableReference selectedExpression,
			String resultVariable,
			ColumnReferenceSource columnReferenceSource,
			SqlSelectionResolver sqlSelectionResolver,
			QueryResultCreationContext creationContext) {
		return new QueryResultCompositeImpl( selectedExpression, resultVariable, this );
	}

	@Override
	public List<Column> collectColumns() {
		throw new NotYetImplementedException(  );
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

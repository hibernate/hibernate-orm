/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import java.util.Collections;
import java.util.List;

import org.hibernate.boot.model.domain.EmbeddedMapping;
import org.hibernate.boot.model.domain.EmbeddedValueMapping;
import org.hibernate.boot.model.domain.PersistentAttributeMapping;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.Representation;
import org.hibernate.metamodel.model.domain.spi.AbstractManagedType;
import org.hibernate.metamodel.model.domain.spi.EmbeddedContainer;
import org.hibernate.metamodel.model.domain.spi.EmbeddedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.InheritanceCapable;
import org.hibernate.metamodel.model.domain.spi.Instantiator;
import org.hibernate.metamodel.model.domain.spi.NavigableRole;
import org.hibernate.metamodel.model.domain.spi.NavigableVisitationStrategy;
import org.hibernate.metamodel.model.domain.spi.PersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.SingularPersistentAttribute;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.sql.ast.produce.result.internal.QueryResultCompositeImpl;
import org.hibernate.sql.ast.produce.result.spi.QueryResult;
import org.hibernate.sql.ast.produce.result.spi.QueryResultCreationContext;
import org.hibernate.sql.ast.produce.result.spi.SqlSelectionResolver;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;
import org.hibernate.type.descriptor.java.internal.EmbeddableJavaDescriptorImpl;
import org.hibernate.type.descriptor.java.spi.EmbeddableJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptorRegistry;

/**
 * @author Steve Ebersole
 */
public class EmbeddedTypeDescriptorImpl<T>
		extends AbstractManagedType<T>
		implements EmbeddedTypeDescriptor<T> {
	private final EmbeddedContainer container;
	private final NavigableRole navigableRole;

	private final Representation representationMode;
	private final Instantiator instantiator;

	public EmbeddedTypeDescriptorImpl(
			EmbeddedMapping embeddedMapping,
			EmbeddedContainer container,
			String localName,
			RuntimeModelCreationContext creationContext) {
		super( resolveJtd( creationContext, embeddedMapping ) );
		this.container = container;
		this.navigableRole = container.getNavigableRole().append( localName );

		setTypeConfiguration( creationContext.getTypeConfiguration() );

		this.representationMode = embeddedMapping.getValueMapping().getExplicitRepresentation();
		this.instantiator = resolveInstantiator( embeddedMapping.getValueMapping(), creationContext );
	}

	private Instantiator resolveInstantiator(
			EmbeddedValueMapping embeddedValueMapping,
			RuntimeModelCreationContext creationContext) {
		Instantiator instantiator = embeddedValueMapping.getExplicitInstantiator();
		if ( instantiator == null ) {
			instantiator = creationContext.getInstantiatorFactory().createEmbeddableInstantiator(
					embeddedValueMapping,
					this,
					// todo (6.0) - resolve reflection optimizer
					null
			);
		}

		return instantiator;
	}

	@SuppressWarnings("unchecked")
	private static <T> EmbeddableJavaDescriptor<T> resolveJtd(RuntimeModelCreationContext creationContext, EmbeddedMapping embeddedMapping) {
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
			RuntimeModelCreationContext creationContext) {
		bindAttributes( embeddedValueMapping, creationContext );
	}

	@SuppressWarnings("AccessStaticViaInstance")
	private void bindAttributes(EmbeddedValueMapping embeddedValueMapping, RuntimeModelCreationContext creationContext) {
		for ( PersistentAttributeMapping attributeMapping : embeddedValueMapping.getDeclaredPersistentAttributes() ) {
			final PersistentAttribute persistentAttribute = attributeMapping.makeRuntimeAttribute(
					this,
					SingularPersistentAttribute.Disposition.NORMAL,
					creationContext
			);

			addAttribute( persistentAttribute );
		}
	}

	@Override
	public void completeInitialization(
			EmbeddedValueMapping embeddedValueMapping,
			RuntimeModelCreationContext creationContext) {
		// todo (6.0) : I think we will want some form of "after all mappings have been 'initialized' (see #finishInstantiation)"
	}

	@Override
	public EmbeddedContainer<?> getContainer() {
		return container;
	}

	@Override
	public Instantiator getInstantiator() {
		return null;
	}

	@Override
	public EmbeddedTypeDescriptor<T> getEmbeddedDescriptor() {
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public EmbeddableJavaDescriptor<T> getJavaTypeDescriptor() {
		return (EmbeddableJavaDescriptor<T>) super.getJavaTypeDescriptor();
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
			SqlSelectionResolver sqlSelectionResolver,
			QueryResultCreationContext creationContext) {
		return new QueryResultCompositeImpl(
				selectedExpression,
				resultVariable,
				this
		);
	}

	@Override
	public List<Column> collectColumns() {
		throw new NotYetImplementedException(  );
	}

	@Override
	public List<InheritanceCapable<? extends T>> getSubclassTypes() {
		return Collections.emptyList();
	}

	@Override
	public void injectSuperTypeDescriptor(InheritanceCapable<? super T> superType) {
		throw new UnsupportedOperationException( "Embeddable inheritance is not yet implemented" );
	}

	@Override
	public void addSubclassType(InheritanceCapable<? extends T> subclassType) {
		throw new UnsupportedOperationException( "Embeddable inheritance is not yet implemented" );
	}
}

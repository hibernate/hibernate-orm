/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.hibernate.boot.model.domain.IdentifiableTypeMapping;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.sql.ast.produce.metamodel.spi.Fetchable;
import org.hibernate.type.descriptor.java.spi.IdentifiableJavaDescriptor;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractIdentifiableType<T> extends AbstractManagedType<T> implements IdentifiableTypeDescriptor<T> {
	public AbstractIdentifiableType(
			IdentifiableTypeMapping bootMapping,
			IdentifiableTypeDescriptor<? super T> superTypeDescriptor,
			IdentifiableJavaDescriptor<T> javaTypeDescriptor,
			RuntimeModelCreationContext creationContext) {
		super( bootMapping, superTypeDescriptor, javaTypeDescriptor, creationContext );

		if ( superTypeDescriptor != null ) {
			superTypeDescriptor.getInFlightAccess().addSubTypeDescriptor( this );
		}
	}

	@Override
	protected IdentifiableTypeDescriptor.InFlightAccess<T> createInFlightAccess() {
		return new InFlightAccessImpl( super.createInFlightAccess() );
	}

	@Override
	public IdentifiableTypeDescriptor.InFlightAccess<T> getInFlightAccess() {
		return (IdentifiableTypeDescriptor.InFlightAccess<T>) super.getInFlightAccess();
	}

	@Override
	public IdentifiableJavaDescriptor<T> getJavaTypeDescriptor() {
		return (IdentifiableJavaDescriptor<T>) super.getJavaTypeDescriptor();
	}

	@SuppressWarnings("unchecked")
	@Override
	public IdentifiableTypeDescriptor<? super T> getSuperclassType() {
		return (IdentifiableTypeDescriptor<? super T>) super.getSuperclassType();
	}

	@Override
	public void visitDeclaredNavigables(NavigableVisitationStrategy visitor) {
		getHierarchy().getIdentifierDescriptor().visitNavigable( visitor );
		super.visitDeclaredNavigables( visitor );
	}

	@Override
	public void visitStateArrayContributors(Consumer<StateArrayContributor<?>> consumer) {

		// todo (6.0) : determine which "root entity" navigables need to be written to the state array.
		//		- also, make sure this only happens for the root

//		if ( getHierarchy().getDiscriminatorDescriptor() != null ) {
//			consumer.accept( getHierarchy().getDiscriminatorDescriptor() );
//		}

//		if ( getHierarchy().getVersionDescriptor() != null ) {
//			consumer.accept( getHierarchy().getVersionDescriptor() );
//		}

//		if ( getHierarchy().getTenantDiscrimination() != null ) {
//			consumer.accept( getHierarchy().getTenantDiscrimination() );
//		}

//		if ( getHierarchy().getRowIdDescriptor() != null ) {
//			consumer.accept( getHierarchy().getRowIdDescriptor() );
//		}

		super.visitStateArrayContributors( consumer );
	}

	@Override
	public void visitKeyFetchables(Consumer<Fetchable> fetchableConsumer) {
		if ( getHierarchy().getIdentifierDescriptor() instanceof NavigableContainer ) {
			( (NavigableContainer<?>) getHierarchy().getIdentifierDescriptor() ).visitFetchables( fetchableConsumer );
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <N> Navigable<N> findDeclaredNavigable(String navigableName) {
		Navigable<Object> navigable = super.findDeclaredNavigable( navigableName );
		if ( navigable == null ) {
			if ( getHierarchy().getRootEntityType().equals( this ) ) {
				navigable = getHierarchy().findNavigable( navigableName );
			}
		}

		return (Navigable<N>) navigable;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Navigable findNavigable(String navigableName) {
		// todo (6.0) : not sure this is the correct way to manage navigable names containing . (see the mapped by in org.hibernate.orm.test.annotations.ci.keymanytoone.Card )
		String[] navigableNames = navigableName.split( "\\." );
		String name = navigableNames[0];
		Navigable navigable = super.findDeclaredNavigable( name );
		if ( navigable == null ) {
			navigable = getHierarchy().findNavigable( name );
		}
		for ( int i = 1; i < navigableNames.length; i++ ) {
			navigable = ( (NavigableContainer) navigable ).findNavigable( navigableNames[i] );
		}

		return navigable;
	}

	protected class InFlightAccessImpl implements IdentifiableTypeDescriptor.InFlightAccess<T> {
		private final AbstractManagedType.InFlightAccess managedTypeAccess;

		private InFlightAccessImpl(ManagedTypeDescriptor.InFlightAccess managedTypeAccess) {
			this.managedTypeAccess = managedTypeAccess;
		}

		@Override
		@SuppressWarnings("unchecked")
		public void addAttribute(PersistentAttributeDescriptor attribute) {
			managedTypeAccess.addAttribute( attribute );
		}

		@Override
		@SuppressWarnings("unchecked")
		public void addSubTypeDescriptor(IdentifiableTypeDescriptor subTypeDescriptor) {
			addSubclassDescriptor( subTypeDescriptor );
		}

		@Override
		public void finishUp() {
			managedTypeAccess.finishUp();
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public void visitSubTypeDescriptors(Consumer<IdentifiableTypeDescriptor<? extends T>> action) {
		final Collection<InheritanceCapable<? extends T>> subclassTypes = getSubclassTypes();

		for ( InheritanceCapable<? extends T> subclassType : subclassTypes ) {
			action.accept( (IdentifiableTypeDescriptor) subclassType );
		}

	}

	@Override
	@SuppressWarnings("unchecked")
	public void visitAllSubTypeDescriptors(Consumer<IdentifiableTypeDescriptor<? extends T>> action) {
		final Collection<IdentifiableTypeDescriptor<? extends T>> subclassTypes = (Collection) getSubclassTypes();
		for ( IdentifiableTypeDescriptor<? extends T> subclassType : subclassTypes ) {
			action.accept( subclassType );
			subclassType.visitAllSubTypeDescriptors( (Consumer) action );
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public IdentifiableTypeDescriptor findMatchingSubTypeDescriptors(Predicate<IdentifiableTypeDescriptor<? extends T>> matcher) {
		if ( matcher.test( this ) ) {
			return this;
		}

		final Collection<IdentifiableTypeDescriptor<? extends T>> subclassTypes = (Collection) getSubclassTypes();
		for ( IdentifiableTypeDescriptor<? extends T> subclassType : subclassTypes ) {
			final IdentifiableTypeDescriptor matched = subclassType.findMatchingSubTypeDescriptors( (Predicate) matcher );
			if ( matched != null ) {
				return matched;
			}
		}

		return null;
	}
}

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import java.util.function.Consumer;

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

		if ( getHierarchy().getVersionDescriptor() != null ) {
			consumer.accept( getHierarchy().getVersionDescriptor() );
		}

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
		Navigable<Object> navigable = super.findDeclaredNavigable( navigableName );
		if ( navigable == null ) {
			navigable = getHierarchy().findNavigable( navigableName );
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
		public void finishUp() {
			managedTypeAccess.finishUp();
		}
	}
}

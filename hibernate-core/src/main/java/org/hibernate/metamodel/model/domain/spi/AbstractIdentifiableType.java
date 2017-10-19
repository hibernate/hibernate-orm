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
	public void visitStateArrayNavigables(Consumer<StateArrayContributor<?>> consumer) {

		// todo (6.0) : determine which "root entity" navigables need to be written to the state array.
		//		- also, make sure this only happens for the root

		if ( getHierarchy().getDiscriminatorDescriptor() != null ) {
			consumer.accept( getHierarchy().getDiscriminatorDescriptor() );
		}

		if ( getHierarchy().getVersionDescriptor() != null ) {
			consumer.accept( getHierarchy().getVersionDescriptor() );
		}

		if ( getHierarchy().getTenantDiscrimination() != null ) {
			consumer.accept( getHierarchy().getTenantDiscrimination() );
		}

//		if ( getHierarchy().getRowIdDescriptor() != null ) {
//			consumer.accept( getHierarchy().getRowIdDescriptor() );
//		}

		super.visitStateArrayNavigables( consumer );
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
}

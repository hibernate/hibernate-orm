/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import org.hibernate.metamodel.model.domain.internal.SingularPersistentAttributeBasic;
import org.hibernate.metamodel.model.domain.internal.SingularPersistentAttributeEmbedded;
import org.hibernate.metamodel.model.domain.internal.SingularPersistentAttributeEntity;

/**
 * Visitation strategy for walking Hibernate's Navigable graph.  Following visitor pattern
 * this contract would serve the role of visitor which each node accepts.
 * <p/>
 * {@link #prepareForVisitation()} and {@link #visitationComplete()} are called at the start
 * and at the finish of the process.
 *
 * @author Steve Ebersole
 */
public interface NavigableVisitationStrategy {
	// todo (6.0) : many methods here deal with internal types - we need to develop API/SPI counterparts to these
	//		^^ API if we want to allow applications to use this - maybe something like:
	//		Session/SessionFactory#visit(String entityName, NavigableVisitationStrategy visitor)
	//		Session/SessionFactory#visit(Class entityJavaType, NavigableVisitationStrategy visitor)

	// todo (6.0) : replace this with NavigableContainer#visitNavigables?

	/**
	 * Notification we are preparing to start visitation.
	 */
	default void prepareForVisitation() {
	}

	/**
	 * Notification we are finished visitation.
	 */
	default void visitationComplete() {
	}

	/**
	 * Visit an entity
	 */
	default void visitEntity(EntityTypeDescriptor entity) {
	}

	/**
	 * Visit an entity's identifier, which is "simple" (single basic attribute)
	 */
	default void visitSimpleIdentifier(EntityIdentifierSimple identifier) {
	}

	/**
	 * Visit an entity's identifier, which is an "aggregated composite (single composite attribute)
	 */
	default void visitAggregateCompositeIdentifier(EntityIdentifierCompositeAggregated identifier) {
	}

	/**
	 * Visit an entity's identifier, which is a "non-aggregated composite (multiple attributes)
	 */
	default void visitNonAggregateCompositeIdentifier(EntityIdentifierCompositeNonAggregated identifier) {
	}

	default void visitDiscriminator(DiscriminatorDescriptor discriminator) {
	}

	default void visitTenantTenantDiscrimination(TenantDiscrimination tenantDiscrimination) {
	}

	default void visitVersion(VersionDescriptor version) {
	}

	default void visitRowIdDescriptor(RowIdDescriptor rowIdDescriptor) {
	}

	default void visitSingularAttributeBasic(SingularPersistentAttributeBasic attribute) {
	}

	default void visitSingularAttributeEmbedded(SingularPersistentAttributeEmbedded attribute) {
	}

	default void visitSingularAttributeEntity(SingularPersistentAttributeEntity attribute) {
	}

	default void visitPluralAttribute(PluralPersistentAttribute attribute) {
		visitCollectionForeignKey( attribute.getPersistentCollectionDescriptor().getCollectionKeyDescriptor() );

		final CollectionIdentifier idDescriptor = attribute.getPersistentCollectionDescriptor().getIdDescriptor();
		if ( idDescriptor != null ) {
			visitCollectionIdentifier( idDescriptor );
		}

		final CollectionIndex indexDescriptor = attribute.getPersistentCollectionDescriptor().getIndexDescriptor();
		if ( indexDescriptor != null ) {
			indexDescriptor.visitNavigable( this );
		}

		attribute.getPersistentCollectionDescriptor().getElementDescriptor().visitNavigable( this );
	}

	default void visitCollectionForeignKey(CollectionKey collectionKey) {
	}

	default void visitCollectionIdentifier(CollectionIdentifier identifier) {
	}

	default void visitCollectionElementBasic(BasicCollectionElement element) {
	}

	default void visitCollectionElementEmbedded(CollectionElementEmbedded element) {
	}

	default void visitCollectionElementEntity(CollectionElementEntity element) {
	}

	default void visitCollectionIndexBasic(BasicCollectionIndex index) {
	}

	default void visitCollectionIndexEmbedded(CollectionIndexEmbedded index) {
	}

	default void visitCollectionIndexEntity(CollectionIndexEntity index) {
	}


}

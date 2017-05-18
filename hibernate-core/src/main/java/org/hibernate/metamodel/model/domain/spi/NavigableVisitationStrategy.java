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

	/**
	 * Notification we are preparing to start visitation.
	 */
	void prepareForVisitation();

	/**
	 * Notification we are finished visitation.
	 */
	void visitationComplete();

	/**
	 * Visit an entity
	 */
	void visitEntity(EntityTypeImplementor entity);

	/**
	 * Visit an entity's identifier, which is "simple" (single basic attribute)
	 */
	void visitSimpleIdentifier(EntityIdentifierSimple identifier);

	/**
	 * Visit an entity's identifier, which is an "aggregated composite (single composite attribute)
	 */
	void visitAggregateCompositeIdentifier(EntityIdentifierCompositeAggregated identifier);

	/**
	 * Visit an entity's identifier, which is a "non-aggregated composite (multiple attributes)
	 */
	void visitNonAggregateCompositeIdentifier(EntityIdentifierCompositeNonAggregated identifier);

	void visitDiscriminator(DiscriminatorDescriptor discriminator);

	void visitTenantTenantDiscrimination(TenantDiscrimination tenantDiscrimination);

	void visitVersion(VersionDescriptor version);

	void visitRowIdDescriptor(RowIdDescriptor rowIdDescriptor);

	void visitSingularAttributeBasic(SingularPersistentAttributeBasic attribute);

	void visitSingularAttributeEmbedded(SingularPersistentAttributeEmbedded attribute);

	void visitSingularAttributeEntity(SingularPersistentAttributeEntity attribute);

	default void visitPluralAttribute(PluralPersistentAttribute attribute) {
		visitCollectionForeignKey( attribute.getPersistentCollectionMetadata().getForeignKeyDescriptor() );

		final CollectionIdentifier idDescriptor = attribute.getPersistentCollectionMetadata().getIdDescriptor();
		if ( idDescriptor != null ) {
			visitCollectionIdentifier( idDescriptor );
		}

		final CollectionIndex indexDescriptor = attribute.getPersistentCollectionMetadata().getIndexDescriptor();
		if ( indexDescriptor != null ) {
			indexDescriptor.visitNavigable( this );
		}

		attribute.getPersistentCollectionMetadata().getElementDescriptor().visitNavigable( this );
	}

	void visitCollectionForeignKey(CollectionKey collectionKey);

	void visitCollectionIdentifier(CollectionIdentifier identifier);

	void visitCollectionElementBasic(CollectionElementBasic element);

	void visitCollectionElementEmbedded(CollectionElementEmbedded element);

	void visitCollectionElementEntity(CollectionElementEntity element);

	void visitCollectionIndexBasic(CollectionIndexBasic index);

	void visitCollectionIndexEmbedded(CollectionIndexEmbedded index);

	void visitCollectionIndexEntity(CollectionIndexEntity index);


}

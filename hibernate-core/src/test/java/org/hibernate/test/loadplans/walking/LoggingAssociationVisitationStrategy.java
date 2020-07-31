/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.loadplans.walking;

import org.hibernate.LockMode;
import org.hibernate.annotations.common.util.StringHelper;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.plan.build.internal.FetchStyleLoadPlanBuildingAssociationVisitationStrategy;
import org.hibernate.loader.plan.spi.FetchSource;
import org.hibernate.loader.plan.spi.LoadPlan;
import org.hibernate.persister.walking.spi.AnyMappingDefinition;
import org.hibernate.persister.walking.spi.AssociationAttributeDefinition;
import org.hibernate.persister.walking.spi.AssociationKey;
import org.hibernate.persister.walking.spi.AttributeDefinition;
import org.hibernate.persister.walking.spi.CollectionDefinition;
import org.hibernate.persister.walking.spi.CollectionElementDefinition;
import org.hibernate.persister.walking.spi.CollectionIndexDefinition;
import org.hibernate.persister.walking.spi.CompositionDefinition;
import org.hibernate.persister.walking.spi.EntityDefinition;
import org.hibernate.persister.walking.spi.EntityIdentifierDefinition;

/**
 * @author Steve Ebersole
 */
public class LoggingAssociationVisitationStrategy extends FetchStyleLoadPlanBuildingAssociationVisitationStrategy {
	private int depth = 1;

	/**
	 * Constructs a FetchStyleLoadPlanBuildingAssociationVisitationStrategy.
	 *
	 * @param sessionFactory       The session factory
	 * @param loadQueryInfluencers The options which can influence the SQL query needed to perform the load.
	 * @param lockMode             The lock mode.
	 */
	public LoggingAssociationVisitationStrategy(
			SessionFactoryImplementor sessionFactory,
			LoadQueryInfluencers loadQueryInfluencers, LockMode lockMode) {
		super( sessionFactory, loadQueryInfluencers, lockMode );
	}

	@Override
	public void start() {
		System.out.println( ">> Start" );
		super.start();
	}

	@Override
	public void finish() {
		System.out.println( "<< Finish" );
		super.finish();
	}

	@Override
	public void startingEntity(EntityDefinition entityDefinition) {
		System.out.println(
				String.format(
						"%s Starting entity (%s)",
						StringHelper.repeat( ">>", ++depth ),
						entityDefinition.getEntityPersister().getEntityName()
				)
		);
		super.startingEntity( entityDefinition );
	}

	@Override
	public void finishingEntity(EntityDefinition entityDefinition) {
		System.out.println(
				String.format(
						"%s Finishing entity (%s)",
						StringHelper.repeat( "<<", depth-- ),
						entityDefinition.getEntityPersister().getEntityName()
				)
		);
		super.finishingEntity( entityDefinition );
	}

	@Override
	public void startingEntityIdentifier(EntityIdentifierDefinition entityIdentifierDefinition) {
		System.out.println(
				String.format(
						"%s Starting [%s] entity identifier (%s)",
						StringHelper.repeat( ">>", ++depth ),
						entityIdentifierDefinition.isEncapsulated() ? "encapsulated" : "non-encapsulated",
						entityIdentifierDefinition.getEntityDefinition().getEntityPersister().getEntityName()
				)
		);
		super.startingEntityIdentifier( entityIdentifierDefinition );
	}

	@Override
	public void finishingEntityIdentifier(EntityIdentifierDefinition entityIdentifierDefinition) {
		System.out.println(
				String.format(
						"%s Finishing entity identifier (%s)",
						StringHelper.repeat( "<<", depth-- ),
						entityIdentifierDefinition.getEntityDefinition().getEntityPersister().getEntityName()
				)
		);
		super.finishingEntityIdentifier( entityIdentifierDefinition );
	}

	@Override
	public boolean startingAttribute(AttributeDefinition attributeDefinition) {
		System.out.println(
				String.format(
						"%s Handling attribute (%s)",
						StringHelper.repeat( ">>", depth + 1 ),
						attributeDefinition.getName()
				)
		);
		return super.startingAttribute( attributeDefinition );
	}

	@Override
	public void finishingAttribute(AttributeDefinition attributeDefinition) {
		super.finishingAttribute( attributeDefinition );
	}

	@Override
	public void startingComposite(CompositionDefinition compositionDefinition) {
		System.out.println(
				String.format(
						"%s Starting composite (%s)",
						StringHelper.repeat( ">>", ++depth ),
						compositionDefinition.getName()
				)
		);
		super.startingComposite( compositionDefinition );
	}

	@Override
	public void finishingComposite(CompositionDefinition compositionDefinition) {
		System.out.println(
				String.format(
						"%s Finishing composite (%s)",
						StringHelper.repeat( "<<", depth-- ),
						compositionDefinition.getName()
				)
		);
		super.finishingComposite( compositionDefinition );
	}

	@Override
	public void startingCollection(CollectionDefinition collectionDefinition) {
		System.out.println(
				String.format(
						"%s Starting collection (%s)",
						StringHelper.repeat( ">>", ++depth ),
						collectionDefinition.getCollectionPersister().getRole()
				)
		);
		super.startingCollection( collectionDefinition );
	}

	@Override
	public void finishingCollection(CollectionDefinition collectionDefinition) {
		System.out.println(
				String.format(
						"%s Finishing collection (%s)",
						StringHelper.repeat( ">>", depth-- ),
						collectionDefinition.getCollectionPersister().getRole()
				)
		);
		super.finishingCollection( collectionDefinition );
	}


	@Override
	public void startingCollectionIndex(CollectionIndexDefinition collectionIndexDefinition) {
		System.out.println(
				String.format(
						"%s Starting collection index (%s)",
						StringHelper.repeat( ">>", ++depth ),
						collectionIndexDefinition.getCollectionDefinition().getCollectionPersister().getRole()
				)
		);
		super.startingCollectionIndex( collectionIndexDefinition );
	}

	@Override
	public void finishingCollectionIndex(CollectionIndexDefinition collectionIndexDefinition) {
		System.out.println(
				String.format(
						"%s Finishing collection index (%s)",
						StringHelper.repeat( "<<", depth-- ),
						collectionIndexDefinition.getCollectionDefinition().getCollectionPersister().getRole()
				)
		);
		super.finishingCollectionIndex( collectionIndexDefinition );
	}

	@Override
	public void startingCollectionElements(CollectionElementDefinition elementDefinition) {
		System.out.println(
				String.format(
						"%s Starting collection elements (%s)",
						StringHelper.repeat( ">>", ++depth ),
						elementDefinition.getCollectionDefinition().getCollectionPersister().getRole()
				)
		);
		super.startingCollectionElements( elementDefinition );
	}

	@Override
	public void finishingCollectionElements(CollectionElementDefinition elementDefinition) {
		System.out.println(
				String.format(
						"%s Finishing collection elements (%s)",
						StringHelper.repeat( "<<", depth-- ),
						elementDefinition.getCollectionDefinition().getCollectionPersister().getRole()
				)
		);
		super.finishingCollectionElements( elementDefinition );
	}

	@Override
	public void foundAny(AnyMappingDefinition anyDefinition) {
		super.foundAny( anyDefinition );
	}

	@Override
	public void associationKeyRegistered(AssociationKey associationKey) {
		System.out.println(
				String.format(
						"%s AssociationKey registered : %s",
						StringHelper.repeat( ">>", depth + 1 ),
						associationKey.toString()
				)
		);
		super.associationKeyRegistered( associationKey );
	}

	@Override
	public FetchSource registeredFetchSource(AssociationKey associationKey) {
		return super.registeredFetchSource( associationKey );
	}

	@Override
	public void foundCircularAssociation(
			AssociationAttributeDefinition attributeDefinition) {
		System.out.println(
				String.format(
						"%s Handling circular association attribute (%s) : %s",
						StringHelper.repeat( ">>", depth + 1 ),
						attributeDefinition.toString(),
						attributeDefinition.getAssociationKey().toString()
				)
		);
		super.foundCircularAssociation( attributeDefinition );
	}

	@Override
	public boolean isDuplicateAssociationKey(AssociationKey associationKey) {
		return super.isDuplicateAssociationKey( associationKey );
	}

	@Override
	public boolean isDuplicateAssociatedEntity(AssociationAttributeDefinition attributeDefinition) {
		return super.isDuplicateAssociatedEntity( attributeDefinition );
	}

	@Override
	public LoadPlan buildLoadPlan() {
		return null;
	}
}

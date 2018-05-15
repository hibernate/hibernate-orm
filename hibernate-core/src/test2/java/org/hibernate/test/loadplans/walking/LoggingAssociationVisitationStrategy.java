/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.loadplans.walking;

import org.hibernate.annotations.common.util.StringHelper;
import org.hibernate.loader.plan.spi.FetchSource;
import org.hibernate.persister.walking.spi.AnyMappingDefinition;
import org.hibernate.persister.walking.spi.AssociationAttributeDefinition;
import org.hibernate.persister.walking.spi.AssociationKey;
import org.hibernate.persister.walking.spi.AssociationVisitationStrategy;
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
public class LoggingAssociationVisitationStrategy implements AssociationVisitationStrategy {
	private int depth = 1;

	@Override
	public void start() {
		System.out.println( ">> Start" );
	}

	@Override
	public void finish() {
		System.out.println( "<< Finish" );
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
		return true;
	}

	@Override
	public void finishingAttribute(AttributeDefinition attributeDefinition) {
		// nothing to do
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
	}

	@Override
	public void foundAny(AnyMappingDefinition anyDefinition) {
		// nothing to do
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
	}

	@Override
	public FetchSource registeredFetchSource(AssociationKey associationKey) {
		return null;
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
	}

	@Override
	public boolean isDuplicateAssociationKey(AssociationKey associationKey) {
		return false;  //To change body of implemented methods use File | Settings | File Templates.
	}

}

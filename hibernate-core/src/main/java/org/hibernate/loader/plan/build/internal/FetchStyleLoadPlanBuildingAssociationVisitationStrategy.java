/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.plan.build.internal;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.loader.plan.spi.CollectionReturn;
import org.hibernate.loader.plan.spi.EntityReturn;
import org.hibernate.loader.plan.spi.FetchSource;
import org.hibernate.loader.plan.spi.LoadPlan;
import org.hibernate.loader.plan.spi.Return;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.walking.spi.AssociationAttributeDefinition;
import org.hibernate.persister.walking.spi.EncapsulatedEntityIdentifierDefinition;
import org.hibernate.persister.walking.spi.EntityIdentifierDefinition;
import org.hibernate.persister.walking.spi.NonEncapsulatedEntityIdentifierDefinition;
import org.hibernate.persister.walking.spi.WalkingException;
import org.hibernate.type.CompositeType;
import org.hibernate.type.EmbeddedComponentType;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;

import org.jboss.logging.Logger;

/**
 * LoadPlanBuildingAssociationVisitationStrategy implementation used for building LoadPlans based on metamodel-defined fetching.  Built
 * LoadPlans contain a single root return object, either an {@link EntityReturn} or a {@link CollectionReturn}.
 *
 * @author Steve Ebersole
 */
public class FetchStyleLoadPlanBuildingAssociationVisitationStrategy
		extends AbstractLoadPlanBuildingAssociationVisitationStrategy {
	private static final Logger log = CoreLogging.logger( FetchStyleLoadPlanBuildingAssociationVisitationStrategy.class );

	private final LoadQueryInfluencers loadQueryInfluencers;
	private final LockMode lockMode;

	private Return rootReturn;

	// flag that indicates whether executing handleAssociationAttribute() should be vetoed;
	private boolean vetoHandleAssociationAttribute;

	/**
	 * Constructs a FetchStyleLoadPlanBuildingAssociationVisitationStrategy.
	 *
	 * @param sessionFactory The session factory
	 * @param loadQueryInfluencers The options which can influence the SQL query needed to perform the load.
	 * @param lockMode The lock mode.
	 */
	public FetchStyleLoadPlanBuildingAssociationVisitationStrategy(
			SessionFactoryImplementor sessionFactory,
			LoadQueryInfluencers loadQueryInfluencers,
			LockMode lockMode) {
		super( sessionFactory );
		this.loadQueryInfluencers = loadQueryInfluencers;
		this.lockMode = lockMode;
	}

	@Override
	protected boolean supportsRootEntityReturns() {
		return true;
	}

	@Override
	protected boolean supportsRootCollectionReturns() {
		return true;
	}

	@Override
	protected void addRootReturn(Return rootReturn) {
		if ( this.rootReturn != null ) {
			throw new HibernateException( "Root return already identified" );
		}
		this.rootReturn = rootReturn;
	}

	@Override
	public void startingEntityIdentifier(EntityIdentifierDefinition identifierDefinition ) {
		if ( vetoHandleAssociationAttribute ) {
			throw new WalkingException( "vetoHandleAssociationAttribute is true when starting startingEntityIdentifier()" );
		}
		vetoHandleAssociationAttribute = shouldVetoHandleAssociationAttributeInId(
				rootReturn,
				identifierDefinition
		);
		super.startingEntityIdentifier( identifierDefinition );
	}

	@Override
	public void finishingEntityIdentifier(EntityIdentifierDefinition identifierDefinition) {
		super.finishingEntityIdentifier( identifierDefinition );
		if ( vetoHandleAssociationAttribute !=
				shouldVetoHandleAssociationAttributeInId( rootReturn, identifierDefinition ) ) {
			throw new WalkingException(
					"vetoHandleAssociationAttribute has unexpected value: " + vetoHandleAssociationAttribute
			);
		}
		vetoHandleAssociationAttribute = false;
	}

	private static boolean shouldVetoHandleAssociationAttributeInId(
			Return rootReturn,
			EntityIdentifierDefinition identifierDefinition) {
		// only check the identifierDefinition for a root EntityReturn.
		if ( EntityReturn.class.isInstance( rootReturn ) ) {
			final EntityIdentifierDefinition rootEntityIdentifierDefinition =
					( (EntityReturn) rootReturn ).getEntityPersister().getEntityKeyDefinition();
			if ( rootEntityIdentifierDefinition == identifierDefinition ) {
				// There are 2 cases where an association in an ID should not be "handled":
				// 1) a composite, encapsulated ID (e.g., @EmbeddedId). In this case, the ID is provided
				//    by the application by Session#get or EntityManager#find. Hibernate uses the
				//    provided ID "as is".
				// 2) a non-encapsulated ID without an @IdClass. In this case, the application provides
				//    an instance of the entity with the ID properties initialized. Hibernate uses
				//    the provided ID properties "as is".
				// In these two cases, it is important that associations in the ID not be "handled"
				// (i.e, joined); doing so can result in unexpected results.
				if ( rootEntityIdentifierDefinition.isEncapsulated() ) {
					final EncapsulatedEntityIdentifierDefinition encapsulated =
							(EncapsulatedEntityIdentifierDefinition ) rootEntityIdentifierDefinition;
					if ( encapsulated.getAttributeDefinition().getType().isComponentType() ) {
						// This is 1) (@EmbeddedId).
						return true;
					}
				}
				else {
					final NonEncapsulatedEntityIdentifierDefinition nonEncapsulated =
							(NonEncapsulatedEntityIdentifierDefinition) rootEntityIdentifierDefinition;
					if ( nonEncapsulated.getSeparateIdentifierMappingClass() == null ) {
						// This is 2) (a non-encapsulated ID without an @IdClass)
						return true;
					}
				}
			}
		}
		return false;
	}

	@Override
	protected boolean handleAssociationAttribute(AssociationAttributeDefinition attributeDefinition) {
		return !vetoHandleAssociationAttribute && super.handleAssociationAttribute( attributeDefinition );
	}

	@Override
	public LoadPlan buildLoadPlan() {
		log.debug( "Building LoadPlan..." );

		if ( EntityReturn.class.isInstance( rootReturn ) ) {
			return new LoadPlanImpl( (EntityReturn) rootReturn, getQuerySpaces() );
		}
		else if ( CollectionReturn.class.isInstance( rootReturn ) ) {
			return new LoadPlanImpl( (CollectionReturn) rootReturn, getQuerySpaces() );
		}
		else {
			throw new IllegalStateException( "Unexpected root Return type : " + rootReturn );
		}
	}

	@Override
	protected FetchStrategy determineFetchStrategy(AssociationAttributeDefinition attributeDefinition) {
		FetchStrategy fetchStrategy = attributeDefinition.determineFetchPlan( loadQueryInfluencers, currentPropertyPath );
		if ( fetchStrategy.getTiming() == FetchTiming.IMMEDIATE && fetchStrategy.getStyle() == FetchStyle.JOIN ) {
			// see if we need to alter the join fetch to another form for any reason
			fetchStrategy = adjustJoinFetchIfNeeded( attributeDefinition, fetchStrategy );
		}
		return fetchStrategy;
	}

	/**
	 * If required by this strategy, returns a different {@link FetchStrategy} from what is specified
	 * for the given association attribute.
	 *
	 * @param attributeDefinition The association attribute definition.
	 * @param fetchStrategy The fetch strategy for <code>attributeDefinition</code>.
	 * @return the {@link FetchStrategy}, possibly adjusted by this strategy.
	 */
	protected FetchStrategy adjustJoinFetchIfNeeded(
			AssociationAttributeDefinition attributeDefinition,
			FetchStrategy fetchStrategy) {
		if ( lockMode.greaterThan( LockMode.READ ) ) {
			return new FetchStrategy( fetchStrategy.getTiming(), FetchStyle.SELECT );
		}

		final Integer maxFetchDepth = sessionFactory().getSessionFactoryOptions().getMaximumFetchDepth();
		if ( maxFetchDepth != null && currentDepth() > maxFetchDepth ) {
			return new FetchStrategy( fetchStrategy.getTiming(), FetchStyle.SELECT );
		}

		final FetchSource currentSource = currentSource();
		final Type attributeType = attributeDefinition.getType();


		if ( attributeType.isCollectionType() ) {
			if ( isTooManyCollections() ) {
				// todo : have this revert to batch or subselect fetching once "sql gen redesign" is in place
				return new FetchStrategy( fetchStrategy.getTiming(), FetchStyle.SELECT );
			}
			if ( currentSource.resolveEntityReference() != null ) {
				CollectionPersister collectionPersister =
						(CollectionPersister) attributeDefinition.getType().getAssociatedJoinable( sessionFactory() );
				// Check if this is an eager "mappedBy" (inverse) side of a bidirectional
				// one-to-many/many-to-one association, with the many-to-one side
				// being the associated entity's ID as in:
				//
				//  @Entity
				//  public class Foo {
				//      ...
				//      @OneToMany(mappedBy = "foo", fetch = FetchType.EAGER)
				//      private Set<Bar> bars = new HashSet<>();
				//  }
				//  @Entity
				//  public class Bar implements Serializable {
				//      @Id
				//      @ManyToOne(fetch = FetchType.EAGER)
				//      private Foo foo;
				//      ...
				//  }
				//
				if ( fetchStrategy.getTiming() == FetchTiming.IMMEDIATE &&
						fetchStrategy.getStyle() == FetchStyle.JOIN &&
						collectionPersister.isOneToMany() &&
						collectionPersister.isInverse() ) {
					// This is an eager "mappedBy" (inverse) side of a bidirectional
					// one-to-many/many-to-one association
					final EntityType elementType = (EntityType) collectionPersister.getElementType();
					final Type elementIdType = ( (EntityPersister) elementType.getAssociatedJoinable( sessionFactory() ) ).getIdentifierType();
					if ( elementIdType.isComponentType() && ( (CompositeType) elementIdType ).isEmbedded() ) {
						final EmbeddedComponentType elementIdTypeEmbedded = (EmbeddedComponentType) elementIdType;
						if ( elementIdTypeEmbedded.getSubtypes().length == 1 &&
								elementIdTypeEmbedded.getPropertyNames()[ 0 ].equals( collectionPersister.getMappedByProperty() ) ) {
							// The associated entity's ID is the other (many-to-one) side of the association.
							// The one-to-many side must be set to FetchMode.SELECT; otherwise,
							// there will be an infinite loop because the current entity
							// would need to be loaded before the associated entity can be loaded,
							// but the associated entity cannot be loaded until after the current
							// entity is loaded (since the current entity is the associated entity's ID).
							return new FetchStrategy( fetchStrategy.getTiming(), FetchStyle.SELECT );
						}
					}
				}
			}
		}

		return fetchStrategy;
	}

	@Override
	protected boolean isTooManyCollections() {
		return CollectionReturn.class.isInstance( rootReturn );
	}
}

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.loader.plan.exec.internal;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jboss.logging.Logger;

import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.hql.internal.NameGenerator;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.loader.CollectionAliases;
import org.hibernate.loader.DefaultEntityAliases;
import org.hibernate.loader.EntityAliases;
import org.hibernate.loader.GeneratedCollectionAliases;
import org.hibernate.loader.plan.spi.BidirectionalEntityFetch;
import org.hibernate.loader.plan.exec.spi.AliasResolutionContext;
import org.hibernate.loader.plan.exec.spi.CollectionReferenceAliases;
import org.hibernate.loader.plan.exec.spi.EntityReferenceAliases;
import org.hibernate.loader.plan.spi.AnyFetch;
import org.hibernate.loader.plan.spi.CollectionReference;
import org.hibernate.loader.plan.spi.CompositeElementGraph;
import org.hibernate.loader.plan.spi.CompositeFetch;
import org.hibernate.loader.plan.spi.CompositeIndexGraph;
import org.hibernate.loader.plan.spi.EntityReference;
import org.hibernate.loader.plan.spi.Fetch;
import org.hibernate.loader.plan.spi.FetchOwner;
import org.hibernate.loader.plan.spi.Return;
import org.hibernate.loader.plan.spi.ScalarReturn;
import org.hibernate.loader.plan.spi.SourceQualifiable;
import org.hibernate.loader.plan2.build.spi.TreePrinterHelper;
import org.hibernate.loader.plan2.spi.LoadPlan;
import org.hibernate.loader.plan2.spi.QuerySpace;
import org.hibernate.loader.spi.JoinableAssociation;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Loadable;
import org.hibernate.persister.walking.spi.WalkingException;
import org.hibernate.type.EntityType;

/**
 * Provides aliases that are used by load queries and ResultSet processors.
 *
 * @author Gail Badner
 * @author Steve Ebersole
 */
public class AliasResolutionContextImpl implements AliasResolutionContext {
	private static final Logger log = CoreLogging.logger( AliasResolutionContextImpl.class );

	private final SessionFactoryImplementor sessionFactory;

	private final Map<Return,String> sourceAliasByReturnMap;
	private final Map<SourceQualifiable,String> sourceQualifiersByReturnMap;

	private final Map<EntityReference,EntityReferenceAliasesImpl> aliasesByEntityReference =
			new HashMap<EntityReference,EntityReferenceAliasesImpl>();
	private final Map<CollectionReference,LoadQueryCollectionAliasesImpl> aliasesByCollectionReference =
			new HashMap<CollectionReference,LoadQueryCollectionAliasesImpl>();
	private final Map<JoinableAssociation,JoinableAssociationAliasesImpl> aliasesByJoinableAssociation =
			new HashMap<JoinableAssociation, JoinableAssociationAliasesImpl>();

	private int currentAliasSuffix;
	private int currentTableAliasUniqueness;

	/**
	 * Constructs a AliasResolutionContextImpl without any source aliases.  This form is used in
	 * non-query (HQL, criteria, etc) contexts.
	 *
	 * @param sessionFactory The session factory
	 */
	public AliasResolutionContextImpl(SessionFactoryImplementor sessionFactory) {
		this( sessionFactory, 0 );
	}

	/**
	 * Constructs a AliasResolutionContextImpl without any source aliases.  This form is used in
	 * non-query (HQL, criteria, etc) contexts.
	 *
	 * @param sessionFactory The session factory
	 * @param suffixSeed The seed value to use for generating the suffix used when generating SQL aliases.
	 */
	public AliasResolutionContextImpl(SessionFactoryImplementor sessionFactory, int suffixSeed) {
		this(
				sessionFactory,
				suffixSeed,
				Collections.<Return,String>emptyMap(),
				Collections.<SourceQualifiable,String>emptyMap()
		);
	}

	/**
	 * Constructs a AliasResolutionContextImpl with source aliases.  See the notes on
	 * {@link org.hibernate.loader.plan.exec.spi.AliasResolutionContext#getSourceAlias(Return)} for discussion of "source aliases".
	 *
	 * @param sessionFactory The session factory
	 * @param suffixSeed The seed value to use for generating the suffix used when generating SQL aliases.
	 * @param sourceAliasByReturnMap Mapping of the source alias for each return (select-clause assigned alias).
	 * @param sourceQualifiersByReturnMap Mapping of source query qualifiers (from-clause assigned alias).
	 */
	public AliasResolutionContextImpl(
			SessionFactoryImplementor sessionFactory,
			int suffixSeed,
			Map<Return, String> sourceAliasByReturnMap,
			Map<SourceQualifiable, String> sourceQualifiersByReturnMap) {
		this.sessionFactory = sessionFactory;
		this.currentAliasSuffix = suffixSeed;
		this.sourceAliasByReturnMap = new HashMap<Return, String>( sourceAliasByReturnMap );
		this.sourceQualifiersByReturnMap = new HashMap<SourceQualifiable, String>( sourceQualifiersByReturnMap );
	}

	@Override
	public String getSourceAlias(Return theReturn) {
		return sourceAliasByReturnMap.get( theReturn );
	}

	@Override
	public String[] resolveScalarColumnAliases(ScalarReturn scalarReturn) {
		final int numberOfColumns = scalarReturn.getType().getColumnSpan( sessionFactory );

		// if the scalar return was assigned an alias in the source query, use that as the basis for generating
		// the SQL aliases
		final String sourceAlias = getSourceAlias( scalarReturn );
		if ( sourceAlias != null ) {
			// generate one based on the source alias
			// todo : to do this properly requires dialect involvement ++
			// 		due to needing uniqueness even across identifier length based truncation; just truncating is
			//		*not* enough since truncated names might clash
			//
			// for now, don't even truncate...
			return NameGenerator.scalarNames( sourceAlias, numberOfColumns );
		}
		else {
			// generate one from scratch
			return NameGenerator.scalarNames( currentAliasSuffix++, numberOfColumns );
		}
	}

	@Override
	public EntityReferenceAliases resolveAliases(EntityReference entityReference) {
		EntityReferenceAliasesImpl aliases = aliasesByEntityReference.get( entityReference );
		if ( aliases == null ) {
			if ( BidirectionalEntityFetch.class.isInstance( entityReference ) ) {
				return resolveAliases(
						( (BidirectionalEntityFetch) entityReference ).getTargetEntityReference()
				);
			}
			final EntityPersister entityPersister = entityReference.getEntityPersister();
			aliases = new EntityReferenceAliasesImpl(
					createTableAlias( entityPersister ),
					createEntityAliases( entityPersister )
			);
			aliasesByEntityReference.put( entityReference, aliases );
		}
		return aliases;
	}

	@Override
	public CollectionReferenceAliases resolveAliases(CollectionReference collectionReference) {
		LoadQueryCollectionAliasesImpl aliases = aliasesByCollectionReference.get( collectionReference );
		if ( aliases == null ) {
			final CollectionPersister collectionPersister = collectionReference.getCollectionPersister();
			aliases = new LoadQueryCollectionAliasesImpl(
					createTableAlias( collectionPersister.getRole() ),
					collectionPersister.isManyToMany()
							? createTableAlias( collectionPersister.getRole() )
							: null,
					createCollectionAliases( collectionPersister ),
					createCollectionElementAliases( collectionPersister )
			);
			aliasesByCollectionReference.put( collectionReference, aliases );
		}
		return aliases;
	}







	@Override
	public String resolveAssociationRhsTableAlias(JoinableAssociation joinableAssociation) {
		return getOrGenerateJoinAssocationAliases( joinableAssociation ).rhsAlias;
	}

	@Override
	public String resolveAssociationLhsTableAlias(JoinableAssociation joinableAssociation) {
		return getOrGenerateJoinAssocationAliases( joinableAssociation ).lhsAlias;
	}

	@Override
	public String[] resolveAssociationAliasedLhsColumnNames(JoinableAssociation joinableAssociation) {
		return getOrGenerateJoinAssocationAliases( joinableAssociation ).aliasedLhsColumnNames;
	}

	protected SessionFactoryImplementor sessionFactory() {
		return sessionFactory;
	}

	private String createSuffix() {
		return Integer.toString( currentAliasSuffix++ ) + '_';
	}

	private JoinableAssociationAliasesImpl getOrGenerateJoinAssocationAliases(JoinableAssociation joinableAssociation) {
		JoinableAssociationAliasesImpl aliases = aliasesByJoinableAssociation.get( joinableAssociation );
		if ( aliases == null ) {
			final Fetch currentFetch = joinableAssociation.getCurrentFetch();
			final String lhsAlias;
			if ( AnyFetch.class.isInstance( currentFetch ) ) {
				throw new WalkingException( "Any type should never be joined!" );
			}
			else if ( EntityReference.class.isInstance( currentFetch.getOwner() ) ) {
				lhsAlias = resolveAliases( (EntityReference) currentFetch.getOwner() ).getTableAlias();
			}
			else if ( CompositeFetch.class.isInstance( currentFetch.getOwner() ) ) {
				lhsAlias = resolveAliases(
						locateCompositeFetchEntityReferenceSource( (CompositeFetch) currentFetch.getOwner() )
				).getTableAlias();
			}
			else if ( CompositeElementGraph.class.isInstance( currentFetch.getOwner() ) ) {
				CompositeElementGraph compositeElementGraph = (CompositeElementGraph) currentFetch.getOwner();
				lhsAlias = resolveAliases( compositeElementGraph.getCollectionReference() ).getElementTableAlias();
			}
			else if ( CompositeIndexGraph.class.isInstance( currentFetch.getOwner() ) ) {
				CompositeIndexGraph compositeIndexGraph = (CompositeIndexGraph) currentFetch.getOwner();
				lhsAlias = resolveAliases( compositeIndexGraph.getCollectionReference() ).getElementTableAlias();
			}
			else {
				throw new NotYetImplementedException( "Cannot determine LHS alias for FetchOwner." );
			}

			final String[] aliasedLhsColumnNames = currentFetch.toSqlSelectFragments( lhsAlias );
			final String rhsAlias;
			if ( EntityReference.class.isInstance( currentFetch ) ) {
				rhsAlias = resolveAliases( (EntityReference) currentFetch ).getTableAlias();
			}
			else if ( CollectionReference.class.isInstance( joinableAssociation.getCurrentFetch() ) ) {
				rhsAlias = resolveAliases( (CollectionReference) currentFetch ).getCollectionTableAlias();
			}
			else {
				throw new NotYetImplementedException( "Cannot determine RHS alis for a fetch that is not an EntityReference or CollectionReference." );
			}

			// TODO: can't this be found in CollectionAliases or EntityAliases? should be moved to AliasResolutionContextImpl

			aliases = new JoinableAssociationAliasesImpl( lhsAlias, aliasedLhsColumnNames, rhsAlias );
			aliasesByJoinableAssociation.put( joinableAssociation, aliases );
		}
		return aliases;
	}

	private EntityReference locateCompositeFetchEntityReferenceSource(CompositeFetch composite) {
		final FetchOwner owner = composite.getOwner();
		if ( EntityReference.class.isInstance( owner ) ) {
			return (EntityReference) owner;
		}
		if ( CompositeFetch.class.isInstance( owner ) ) {
			return locateCompositeFetchEntityReferenceSource( (CompositeFetch) owner );
		}

		throw new WalkingException( "Cannot resolve entity source for a CompositeFetch" );
	}

	private String createTableAlias(EntityPersister entityPersister) {
		return createTableAlias( StringHelper.unqualifyEntityName( entityPersister.getEntityName() ) );
	}

	private String createTableAlias(String name) {
		return StringHelper.generateAlias( name, currentTableAliasUniqueness++ );
	}

	private EntityAliases createEntityAliases(EntityPersister entityPersister) {
		return new DefaultEntityAliases( (Loadable) entityPersister, createSuffix() );
	}

	private CollectionAliases createCollectionAliases(CollectionPersister collectionPersister) {
		return new GeneratedCollectionAliases( collectionPersister, createSuffix() );
	}

	private EntityAliases createCollectionElementAliases(CollectionPersister collectionPersister) {
		if ( !collectionPersister.getElementType().isEntityType() ) {
			return null;
		}
		else {
			final EntityType entityElementType = (EntityType) collectionPersister.getElementType();
			return createEntityAliases( (EntityPersister) entityElementType.getAssociatedJoinable( sessionFactory() ) );
		}
	}

	private static class LoadQueryCollectionAliasesImpl implements CollectionReferenceAliases {
		private final String tableAlias;
		private final String manyToManyAssociationTableAlias;
		private final CollectionAliases collectionAliases;
		private final EntityAliases entityElementAliases;

		public LoadQueryCollectionAliasesImpl(
				String tableAlias,
				String manyToManyAssociationTableAlias,
				CollectionAliases collectionAliases,
				EntityAliases entityElementAliases) {
			this.tableAlias = tableAlias;
			this.manyToManyAssociationTableAlias = manyToManyAssociationTableAlias;
			this.collectionAliases = collectionAliases;
			this.entityElementAliases = entityElementAliases;
		}

		@Override
		public String getCollectionTableAlias() {
			return StringHelper.isNotEmpty( manyToManyAssociationTableAlias )
					? manyToManyAssociationTableAlias
					: tableAlias;
		}

		@Override
		public String getElementTableAlias() {
			return tableAlias;
		}

		@Override
		public CollectionAliases getCollectionColumnAliases() {
			return collectionAliases;
		}

		@Override
		public EntityAliases getEntityElementColumnAliases() {
			return entityElementAliases;
		}
	}

	private static class JoinableAssociationAliasesImpl {
		private final String lhsAlias;
		private final String[] aliasedLhsColumnNames;
		private final String rhsAlias;

		public JoinableAssociationAliasesImpl(
				String lhsAlias,
				String[] aliasedLhsColumnNames,
				String rhsAlias) {
			this.lhsAlias = lhsAlias;
			this.aliasedLhsColumnNames = aliasedLhsColumnNames;
			this.rhsAlias = rhsAlias;
		}
	}
}

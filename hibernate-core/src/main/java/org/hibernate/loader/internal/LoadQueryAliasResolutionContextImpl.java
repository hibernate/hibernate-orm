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
package org.hibernate.loader.internal;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.engine.internal.JoinHelper;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.loader.CollectionAliases;
import org.hibernate.loader.DefaultEntityAliases;
import org.hibernate.loader.EntityAliases;
import org.hibernate.loader.GeneratedCollectionAliases;
import org.hibernate.loader.plan.spi.CollectionReference;
import org.hibernate.loader.plan.spi.CollectionReturn;
import org.hibernate.loader.plan.spi.EntityReference;
import org.hibernate.loader.plan.spi.EntityReturn;
import org.hibernate.loader.plan.spi.Fetch;
import org.hibernate.loader.plan.spi.Return;
import org.hibernate.loader.plan.spi.ScalarReturn;
import org.hibernate.loader.spi.JoinableAssociation;
import org.hibernate.loader.spi.LoadQueryAliasResolutionContext;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Loadable;
import org.hibernate.persister.entity.OuterJoinLoadable;
import org.hibernate.type.EntityType;

/**
 * @author Gail Badner
 */
public class LoadQueryAliasResolutionContextImpl implements LoadQueryAliasResolutionContext {
	private final Map<Return,String[]> aliasesByReturn;
	private final Map<EntityReference,LoadQueryEntityAliasesImpl> aliasesByEntityReference =
			new HashMap<EntityReference,LoadQueryEntityAliasesImpl>();
	private final Map<CollectionReference,LoadQueryCollectionAliasesImpl> aliasesByCollectionReference =
			new HashMap<CollectionReference,LoadQueryCollectionAliasesImpl>();
	private final Map<JoinableAssociation,JoinableAssociationAliasesImpl> aliasesByJoinableAssociation =
			new HashMap<JoinableAssociation, JoinableAssociationAliasesImpl>();
	private final SessionFactoryImplementor sessionFactory;

	private int currentAliasSuffix = 0;

	public LoadQueryAliasResolutionContextImpl(
			SessionFactoryImplementor sessionFactory,
			int suffixSeed,
			Map<Return,String[]> aliasesByReturn) {
		this.sessionFactory = sessionFactory;
		this.currentAliasSuffix = suffixSeed;

		checkAliasesByReturn( aliasesByReturn );
		this.aliasesByReturn = new HashMap<Return, String[]>( aliasesByReturn );
	}

	private static void checkAliasesByReturn(Map<Return, String[]> aliasesByReturn) {
		if ( aliasesByReturn == null || aliasesByReturn.size() == 0 ) {
			throw new IllegalArgumentException( "No return aliases defined" );
		}
		for ( Map.Entry<Return,String[]> entry : aliasesByReturn.entrySet() ) {
			final Return aReturn = entry.getKey();
			final String[] aliases = entry.getValue();
			if ( aReturn == null ) {
				throw new IllegalArgumentException( "null key found in aliasesByReturn" );
			}
			if ( aliases == null || aliases.length == 0 ) {
				throw new IllegalArgumentException(
						String.format( "No alias defined for [%s]", aReturn )
				);
			}
			if ( ( aliases.length > 1 ) &&
					( aReturn instanceof EntityReturn || aReturn instanceof CollectionReturn ) ) {
				throw new IllegalArgumentException( String.format( "More than 1 alias defined for [%s]", aReturn ) );
			}
			for ( String alias : aliases ) {
				if ( StringHelper.isEmpty( alias ) ) {
					throw new IllegalArgumentException( String.format( "An alias for [%s] is null or empty.", aReturn ) );
				}
			}
		}
	}

	@Override
	public String resolveEntityReturnAlias(EntityReturn entityReturn) {
		return getAndCheckReturnAliasExists( entityReturn )[ 0 ];
	}

	@Override
	public String resolveCollectionReturnAlias(CollectionReturn collectionReturn) {
		return getAndCheckReturnAliasExists( collectionReturn )[ 0 ];
	}

	@Override
	public String[] resolveScalarReturnAliases(ScalarReturn scalarReturn) {
		throw new NotYetImplementedException( "Cannot resolve scalar column aliases yet." );
	}

	private String[] getAndCheckReturnAliasExists(Return aReturn) {
		// There is already a check for the appropriate number of aliases stored in aliasesByReturn,
		// so just check for existence here.
		final String[] aliases = aliasesByReturn.get( aReturn );
		if ( aliases == null ) {
			throw new IllegalStateException(
					String.format( "No alias is defined for [%s]", aReturn )
			);
		}
		return aliases;
	}

	@Override
	public String resolveEntitySqlTableAlias(EntityReference entityReference) {
		return getOrGenerateLoadQueryEntityAliases( entityReference ).tableAlias;
	}

	@Override
	public EntityAliases resolveEntityColumnAliases(EntityReference entityReference) {
		return getOrGenerateLoadQueryEntityAliases( entityReference ).columnAliases;
	}

	@Override
	public String resolveCollectionSqlTableAlias(CollectionReference collectionReference) {
		return getOrGenerateLoadQueryCollectionAliases( collectionReference ).tableAlias;
	}

	@Override
	public CollectionAliases resolveCollectionColumnAliases(CollectionReference collectionReference) {
		return getOrGenerateLoadQueryCollectionAliases( collectionReference ).collectionAliases;
	}

	@Override
	public EntityAliases resolveCollectionElementColumnAliases(CollectionReference collectionReference) {
		return getOrGenerateLoadQueryCollectionAliases( collectionReference ).collectionElementAliases;
	}

	@Override
	public String resolveRhsAlias(JoinableAssociation joinableAssociation) {
		return getOrGenerateJoinAssocationAliases( joinableAssociation ).rhsAlias;
	}

	@Override
	public String resolveLhsAlias(JoinableAssociation joinableAssociation) {
		return getOrGenerateJoinAssocationAliases( joinableAssociation ).lhsAlias;
	}

	@Override
	public String[] resolveAliasedLhsColumnNames(JoinableAssociation joinableAssociation) {
		return getOrGenerateJoinAssocationAliases( joinableAssociation ).aliasedLhsColumnNames;
	}

	@Override
	public EntityAliases resolveCurrentEntityAliases(JoinableAssociation joinableAssociation) {
		return joinableAssociation.getCurrentEntityReference() == null ?
				null:
				resolveEntityColumnAliases( joinableAssociation.getCurrentEntityReference() );
	}

	@Override
	public CollectionAliases resolveCurrentCollectionAliases(JoinableAssociation joinableAssociation) {
		return joinableAssociation.getCurrentCollectionReference() == null ?
				null:
				resolveCollectionColumnAliases( joinableAssociation.getCurrentCollectionReference() );
	}

	protected SessionFactoryImplementor sessionFactory() {
		return sessionFactory;
	}

	private String createSuffix() {
		return Integer.toString( currentAliasSuffix++ ) + '_';
	}

	private LoadQueryEntityAliasesImpl getOrGenerateLoadQueryEntityAliases(EntityReference entityReference) {
		LoadQueryEntityAliasesImpl aliases = aliasesByEntityReference.get( entityReference );
		if ( aliases == null ) {
			final EntityPersister entityPersister = entityReference.getEntityPersister();
			aliases = new LoadQueryEntityAliasesImpl(
					createTableAlias( entityPersister ),
					createEntityAliases( entityPersister )
			);
			aliasesByEntityReference.put( entityReference, aliases );
		}
		return aliases;
	}

	private LoadQueryCollectionAliasesImpl getOrGenerateLoadQueryCollectionAliases(CollectionReference collectionReference) {
		LoadQueryCollectionAliasesImpl aliases = aliasesByCollectionReference.get( collectionReference );
		if ( aliases == null ) {
			final CollectionPersister collectionPersister = collectionReference.getCollectionPersister();
			aliases = new LoadQueryCollectionAliasesImpl(
					createTableAlias( collectionPersister.getRole() ),
					createCollectionAliases( collectionPersister ),
					createCollectionElementAliases( collectionPersister )
			);
			aliasesByCollectionReference.put( collectionReference, aliases );
		}
		return aliases;
	}

	private JoinableAssociationAliasesImpl getOrGenerateJoinAssocationAliases(JoinableAssociation joinableAssociation) {
		JoinableAssociationAliasesImpl aliases = aliasesByJoinableAssociation.get( joinableAssociation );
		if ( aliases == null ) {
			final Fetch currentFetch = joinableAssociation.getCurrentFetch();
			final String lhsAlias;
			if ( EntityReference.class.isInstance( currentFetch.getOwner() ) ) {
				lhsAlias = resolveEntitySqlTableAlias( (EntityReference) currentFetch.getOwner() );
			}
			else {
				throw new NotYetImplementedException( "Cannot determine LHS alias for a FetchOwner that is not an EntityReference yet." );
			}
			final String rhsAlias;
			if ( EntityReference.class.isInstance( currentFetch ) ) {
				rhsAlias = resolveEntitySqlTableAlias( (EntityReference) currentFetch );
			}
			else if ( CollectionReference.class.isInstance( joinableAssociation.getCurrentFetch() ) ) {
				rhsAlias = resolveCollectionSqlTableAlias( (CollectionReference) currentFetch );
			}
			else {
				throw new NotYetImplementedException( "Cannot determine RHS alis for a fetch that is not an EntityReference or CollectionReference." );
			}

			// TODO: can't this be found in CollectionAliases or EntityAliases? should be moved to LoadQueryAliasResolutionContextImpl
			final OuterJoinLoadable fetchSourcePersister = (OuterJoinLoadable) currentFetch.getOwner().retrieveFetchSourcePersister();
			final int propertyNumber = fetchSourcePersister.getEntityMetamodel().getPropertyIndex( currentFetch.getOwnerPropertyName() );
			final String[] aliasedLhsColumnNames = JoinHelper.getAliasedLHSColumnNames(
					joinableAssociation.getJoinableType(),
					lhsAlias,
					propertyNumber,
					fetchSourcePersister,
					sessionFactory
			);

			aliases = new JoinableAssociationAliasesImpl( lhsAlias, aliasedLhsColumnNames, rhsAlias );
			aliasesByJoinableAssociation.put( joinableAssociation, aliases );
		}
		return aliases;
	}

	private String createTableAlias(EntityPersister entityPersister) {
		return createTableAlias( StringHelper.unqualifyEntityName( entityPersister.getEntityName() ) );
	}

	private String createTableAlias(String name) {
		return StringHelper.generateAlias( name ) + createSuffix();
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

	private static class LoadQueryEntityAliasesImpl {
		private final String tableAlias;
		private final EntityAliases columnAliases;

		public LoadQueryEntityAliasesImpl(String tableAlias, EntityAliases columnAliases) {
			this.tableAlias = tableAlias;
			this.columnAliases = columnAliases;
		}
	}

	private static class LoadQueryCollectionAliasesImpl {
		private final String tableAlias;
		private final CollectionAliases collectionAliases;
		private final EntityAliases collectionElementAliases;

		public LoadQueryCollectionAliasesImpl(
				String tableAlias,
				CollectionAliases collectionAliases,
				EntityAliases collectionElementAliases) {
			this.tableAlias = tableAlias;
			this.collectionAliases = collectionAliases;
			this.collectionElementAliases = collectionElementAliases;
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

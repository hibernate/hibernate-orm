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
package org.hibernate.loader.plan2.exec.internal;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import org.jboss.logging.Logger;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.loader.CollectionAliases;
import org.hibernate.loader.DefaultEntityAliases;
import org.hibernate.loader.EntityAliases;
import org.hibernate.loader.GeneratedCollectionAliases;
import org.hibernate.loader.plan2.build.spi.QuerySpaceTreePrinter;
import org.hibernate.loader.plan2.build.spi.TreePrinterHelper;
import org.hibernate.loader.plan2.exec.spi.AliasResolutionContext;
import org.hibernate.loader.plan2.exec.spi.CollectionReferenceAliases;
import org.hibernate.loader.plan2.exec.spi.EntityReferenceAliases;
import org.hibernate.loader.plan2.spi.Join;
import org.hibernate.loader.plan2.spi.LoadPlan;
import org.hibernate.loader.plan2.spi.QuerySpace;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Loadable;
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

	// Used to generate unique selection value aliases (column/formula renames)
	private int currentAliasSuffix;
	// Used to generate unique table aliases
	private int currentTableAliasSuffix;

	private Map<String,EntityReferenceAliases> entityReferenceAliasesMap;
	private Map<String,CollectionReferenceAliases> collectionReferenceAliasesMap;
	private Map<String,String> querySpaceUidToSqlTableAliasMap;

	private Map<String,String> compositeQuerySpaceUidToSqlTableAliasMap;

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
	 * <p/>
	 * See the notes on
	 * {@link org.hibernate.loader.plan2.exec.spi.AliasResolutionContext#getSourceAlias} for discussion of
	 * "source aliases".  They are not implemented here yet.
	 *
	 * @param sessionFactory The session factory
	 * @param suffixSeed The seed value to use for generating the suffix used when generating SQL aliases.
	 */
	public AliasResolutionContextImpl(SessionFactoryImplementor sessionFactory, int suffixSeed) {
		this.sessionFactory = sessionFactory;
		this.currentAliasSuffix = suffixSeed;
	}

	protected SessionFactoryImplementor sessionFactory() {
		return sessionFactory;
	}

	public EntityReferenceAliases generateEntityReferenceAliases(String uid, EntityPersister entityPersister) {
		final EntityReferenceAliasesImpl entityReferenceAliases = new EntityReferenceAliasesImpl(
				createTableAlias( entityPersister ),
				createEntityAliases( entityPersister )
		);
		registerQuerySpaceAliases( uid, entityReferenceAliases );
		return entityReferenceAliases;
	}

	private String createTableAlias(EntityPersister entityPersister) {
		return createTableAlias( StringHelper.unqualifyEntityName( entityPersister.getEntityName() ) );
	}

	private String createTableAlias(String name) {
		return StringHelper.generateAlias( name, currentTableAliasSuffix++ );
	}

	private EntityAliases createEntityAliases(EntityPersister entityPersister) {
		return new DefaultEntityAliases( (Loadable) entityPersister, createSuffix() );
	}

	private String createSuffix() {
		return Integer.toString( currentAliasSuffix++ ) + '_';
	}

	public CollectionReferenceAliases generateCollectionReferenceAliases(String uid, CollectionPersister persister) {
		final String manyToManyTableAlias = persister.isManyToMany()? createTableAlias( persister.getRole() ) : null;
		final String tableAlias = createTableAlias( persister.getRole() );
		final CollectionReferenceAliasesImpl aliases = new CollectionReferenceAliasesImpl(
				tableAlias,
				manyToManyTableAlias,
				createCollectionAliases( persister ),
				createCollectionElementAliases( persister )
		);

		registerQuerySpaceAliases( uid, aliases );
		return aliases;
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

	public void registerQuerySpaceAliases(String querySpaceUid, EntityReferenceAliases entityReferenceAliases) {
		if ( entityReferenceAliasesMap == null ) {
			entityReferenceAliasesMap = new HashMap<String, EntityReferenceAliases>();
		}
		entityReferenceAliasesMap.put( querySpaceUid, entityReferenceAliases );
		registerSqlTableAliasMapping( querySpaceUid, entityReferenceAliases.getTableAlias() );
	}

	public void registerSqlTableAliasMapping(String querySpaceUid, String sqlTableAlias) {
		if ( querySpaceUidToSqlTableAliasMap == null ) {
			querySpaceUidToSqlTableAliasMap = new HashMap<String, String>();
		}
		String old = querySpaceUidToSqlTableAliasMap.put( querySpaceUid, sqlTableAlias );
		if ( old != null ) {
			if ( old.equals( sqlTableAlias ) ) {
				// silently ignore...
			}
			else {
				throw new IllegalStateException(
						String.format(
								"Attempt to register multiple SQL table aliases [%s, %s, etc] against query space uid [%s]",
								old,
								sqlTableAlias,
								querySpaceUid
						)
				);
			}
		}
	}

	@Override
	public String resolveSqlTableAliasFromQuerySpaceUid(String querySpaceUid) {
		String alias = null;
		if ( querySpaceUidToSqlTableAliasMap != null ) {
			alias = querySpaceUidToSqlTableAliasMap.get( querySpaceUid );
		}

		if ( alias == null ) {
			if ( compositeQuerySpaceUidToSqlTableAliasMap != null ) {
				alias = compositeQuerySpaceUidToSqlTableAliasMap.get( querySpaceUid );
			}
		}

		return alias;
	}

	@Override
	public EntityReferenceAliases resolveEntityReferenceAliases(String querySpaceUid) {
		return entityReferenceAliasesMap == null ? null : entityReferenceAliasesMap.get( querySpaceUid );
	}

	public void registerQuerySpaceAliases(String querySpaceUid, CollectionReferenceAliases collectionReferenceAliases) {
		if ( collectionReferenceAliasesMap == null ) {
			collectionReferenceAliasesMap = new HashMap<String, CollectionReferenceAliases>();
		}
		collectionReferenceAliasesMap.put( querySpaceUid, collectionReferenceAliases );
		registerSqlTableAliasMapping( querySpaceUid, collectionReferenceAliases.getCollectionTableAlias() );
	}

	@Override
	public CollectionReferenceAliases resolveCollectionReferenceAliases(String querySpaceUid) {
		return collectionReferenceAliasesMap == null ? null : collectionReferenceAliasesMap.get( querySpaceUid );
	}

	public void registerCompositeQuerySpaceUidResolution(String rightHandSideUid, String leftHandSideTableAlias) {
		if ( compositeQuerySpaceUidToSqlTableAliasMap == null ) {
			compositeQuerySpaceUidToSqlTableAliasMap = new HashMap<String, String>();
		}
		compositeQuerySpaceUidToSqlTableAliasMap.put( rightHandSideUid, leftHandSideTableAlias );
	}

	/**
	 * USes its defined logger to generate a resolution report.
	 *
	 * @param loadPlan The loadplan that was processed.
	 */
	public void dumpResolutions(LoadPlan loadPlan) {
		if ( log.isDebugEnabled() ) {
			final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
			final PrintStream printStream = new PrintStream( byteArrayOutputStream );
			final PrintWriter printWriter = new PrintWriter( printStream );

			printWriter.println( "LoadPlan QuerySpace resolutions" );

			for ( QuerySpace querySpace : loadPlan.getQuerySpaces().getRootQuerySpaces() ) {
				dumpQuerySpace( querySpace, 1, printWriter );
			}

			printWriter.flush();
			printStream.flush();

			log.debug( new String( byteArrayOutputStream.toByteArray() ) );
		}
	}

	private void dumpQuerySpace(QuerySpace querySpace, int depth, PrintWriter printWriter) {
		generateDetailLines( querySpace, depth, printWriter );
		dumpJoins( querySpace.getJoins(), depth + 1, printWriter );
	}

	private void generateDetailLines(QuerySpace querySpace, int depth, PrintWriter printWriter) {
		printWriter.println(
				TreePrinterHelper.INSTANCE.generateNodePrefix( depth )
						+ querySpace.getUid() + " -> " + extractDetails( querySpace )
		);
		printWriter.println(
				TreePrinterHelper.INSTANCE.generateNodePrefix( depth+3 )
						+ "SQL table alias mapping - " + resolveSqlTableAliasFromQuerySpaceUid( querySpace.getUid() )
		);

		final EntityReferenceAliases entityAliases = resolveEntityReferenceAliases( querySpace.getUid() );
		final CollectionReferenceAliases collectionReferenceAliases = resolveCollectionReferenceAliases( querySpace.getUid() );

		if ( entityAliases != null ) {
			printWriter.println(
					TreePrinterHelper.INSTANCE.generateNodePrefix( depth+3 )
							+ "alias suffix - " + entityAliases.getColumnAliases().getSuffix()
			);
			printWriter.println(
					TreePrinterHelper.INSTANCE.generateNodePrefix( depth+3 )
							+ "suffixed key columns - "
							+ StringHelper.join( ", ", entityAliases.getColumnAliases().getSuffixedKeyAliases() )
			);
		}

		if ( collectionReferenceAliases != null ) {
			printWriter.println(
					TreePrinterHelper.INSTANCE.generateNodePrefix( depth+3 )
							+ "alias suffix - " + collectionReferenceAliases.getCollectionColumnAliases().getSuffix()
			);
			printWriter.println(
					TreePrinterHelper.INSTANCE.generateNodePrefix( depth+3 )
							+ "suffixed key columns - "
							+ StringHelper.join( ", ", collectionReferenceAliases.getCollectionColumnAliases().getSuffixedKeyAliases() )
			);
			final EntityAliases elementAliases = collectionReferenceAliases.getEntityElementColumnAliases();
			if ( elementAliases != null ) {
				printWriter.println(
						TreePrinterHelper.INSTANCE.generateNodePrefix( depth+3 )
								+ "entity-element alias suffix - " + elementAliases.getSuffix()
				);
				printWriter.println(
						TreePrinterHelper.INSTANCE.generateNodePrefix( depth+3 )
								+ elementAliases.getSuffix()
								+ "entity-element suffixed key columns - "
								+ StringHelper.join( ", ", elementAliases.getSuffixedKeyAliases() )
				);
			}
		}
	}

	private String extractDetails(QuerySpace querySpace) {
		return QuerySpaceTreePrinter.INSTANCE.extractDetails( querySpace );
	}

	private void dumpJoins(Iterable<Join> joins, int depth, PrintWriter printWriter) {
		for ( Join join : joins ) {
			printWriter.println(
					TreePrinterHelper.INSTANCE.generateNodePrefix( depth )
							+ "JOIN (" + join.getLeftHandSide().getUid() + " -> " + join.getRightHandSide()
							.getUid() + ")"
			);
			dumpQuerySpace( join.getRightHandSide(), depth+1, printWriter );
		}
	}
}

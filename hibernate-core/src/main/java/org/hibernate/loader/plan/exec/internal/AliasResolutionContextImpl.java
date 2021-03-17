/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.plan.exec.internal;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.loader.CollectionAliases;
import org.hibernate.loader.DefaultEntityAliases;
import org.hibernate.loader.EntityAliases;
import org.hibernate.loader.GeneratedCollectionAliases;
import org.hibernate.loader.internal.AliasConstantsHelper;
import org.hibernate.loader.plan.build.spi.QuerySpaceTreePrinter;
import org.hibernate.loader.plan.build.spi.TreePrinterHelper;
import org.hibernate.loader.plan.exec.spi.AliasResolutionContext;
import org.hibernate.loader.plan.exec.spi.CollectionReferenceAliases;
import org.hibernate.loader.plan.exec.spi.EntityReferenceAliases;
import org.hibernate.loader.plan.spi.Join;
import org.hibernate.loader.plan.spi.LoadPlan;
import org.hibernate.loader.plan.spi.QuerySpace;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Loadable;
import org.hibernate.type.EntityType;

import org.jboss.logging.Logger;

import static org.hibernate.internal.util.StringHelper.safeInterning;

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

	// Mapping from query space UID to entity reference aliases
	private Map<String,EntityReferenceAliases> entityReferenceAliasesMap;

	// Mapping from query space UID to collection reference aliases
	private Map<String,CollectionReferenceAliases> collectionReferenceAliasesMap;

	// Mapping from query space UID to SQL table alias
	private Map<String,String> querySpaceUidToSqlTableAliasMap;

	// Mapping from composite query space UID to SQL table alias
	private Map<String,String> compositeQuerySpaceUidToSqlTableAliasMap;

	/**
	 * Constructs a {@link AliasResolutionContextImpl} without any source aliases.  This form is used in
	 * non-query contexts. Example of query contexts are: HQL, criteria, etc.
	 *
	 * @param sessionFactory The session factory
	 */
	public AliasResolutionContextImpl(SessionFactoryImplementor sessionFactory) {
		this( sessionFactory, 0 );
	}

	/**
	 * Constructs a AliasResolutionContextImpl with the specified seed for unique alias suffixes.
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

	/**
	 * Generate the entity reference aliases for a particular {@link org.hibernate.loader.plan.spi.EntityReference}
	 * and register the generated value using the query space UID.
	 * <p/>
	 * Once generated, there are two methods that can be used to do look ups by the specified
	 * query space UID:
	 * <ul>
	 * <li>
	 *     {@link #resolveEntityReferenceAliases(String)} can be used to
	 *     look up the returned entity reference aliases;
	 * </li>
	 * <li>
	 *     {@link #resolveSqlTableAliasFromQuerySpaceUid(String)} can be used to
	 *     look up SQL table alias.
	 * </li>
	 * </ul>
	 *
	 * @param uid The query space UID for the entity reference.
	 * @param entityPersister The entity persister for entity reference.
	 * @return the generated entity reference aliases.
	 *
	 * @see org.hibernate.loader.plan.spi.EntityReference#getQuerySpaceUid()
	 * @see org.hibernate.loader.plan.spi.EntityReference#getEntityPersister()
	 */
	public EntityReferenceAliases generateEntityReferenceAliases(String uid, EntityPersister entityPersister) {
		return generateEntityReferenceAliases( uid, createTableAlias( entityPersister ), entityPersister );
	}

	private EntityReferenceAliases generateEntityReferenceAliases(
			String uid,
			String tableAlias,
			EntityPersister entityPersister) {
		final EntityReferenceAliasesImpl entityReferenceAliases = new EntityReferenceAliasesImpl(
				tableAlias,
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
		return AliasConstantsHelper.get( currentAliasSuffix++ );
	}

	/**
	 * Generate the collection reference aliases for a particular {@link org.hibernate.loader.plan.spi.CollectionReference}
	 * and register the generated value using the query space UID.
	 * <p/>
	 * Once generated, there are two methods that can be used to do look ups by the specified
	 * query space UID:
	 * <ul>
	 * <li>
	 *     {@link #resolveCollectionReferenceAliases(String)} can be used to
	 *     look up the returned collection reference aliases;
	 * </li>
	 * <li>
	 *     {@link #resolveSqlTableAliasFromQuerySpaceUid(String)} can be used to
	 *     look up the SQL collection table alias.
	 * </li>
	 * </ul>
	 *
	 * @param collectionQuerySpaceUid The query space UID for the collection reference.
	 * @param persister The collection persister for collection reference.
	 * @param elementQuerySpaceUid The query space UID for the collection element if
	 *                             the element is an entity type; null, otherwise.
	 * @return the generated collection reference aliases.
	 * @throws IllegalArgumentException if the collection element is an entity type and
	 *         {@code elementQuerySpaceUid} is null.
	 *
	 * @see org.hibernate.loader.plan.spi.CollectionReference#getQuerySpaceUid()
	 * @see org.hibernate.loader.plan.spi.CollectionReference#getCollectionPersister()
	 */
	public CollectionReferenceAliases generateCollectionReferenceAliases(
			String collectionQuerySpaceUid,
			CollectionPersister persister,
			String elementQuerySpaceUid) {
		if ( persister.getElementType().isEntityType() && elementQuerySpaceUid == null ) {
			throw new IllegalArgumentException(
					"elementQuerySpaceUid must be non-null for one-to-many or many-to-many associations."
			);
		}

		final String manyToManyTableAlias;
		final String tableAlias;
		if ( persister.isManyToMany() ) {
			manyToManyTableAlias = createTableAlias( persister.getRole() );
			tableAlias = createTableAlias( persister.getElementDefinition().toEntityDefinition().getEntityPersister() );
		}
		else {
			manyToManyTableAlias = null;
			tableAlias = createTableAlias( persister.getRole() );
		}

		final CollectionReferenceAliases collectionAliases = new CollectionReferenceAliasesImpl(
				tableAlias,
				manyToManyTableAlias,
				createCollectionAliases( persister ),
				createCollectionElementAliases( persister, tableAlias, elementQuerySpaceUid )
		);

		registerQuerySpaceAliases( collectionQuerySpaceUid, collectionAliases );
		return collectionAliases;
	}

	private CollectionAliases createCollectionAliases(CollectionPersister collectionPersister) {
		return new GeneratedCollectionAliases( collectionPersister, createSuffix() );
	}

	private EntityReferenceAliases createCollectionElementAliases(
			CollectionPersister collectionPersister,
			String tableAlias,
			String elementQuerySpaceUid) {

		if ( !collectionPersister.getElementType().isEntityType() ) {
			return null;
		}
		else {
			final EntityType entityElementType = (EntityType) collectionPersister.getElementType();
			return generateEntityReferenceAliases(
					elementQuerySpaceUid,
					tableAlias,
					(EntityPersister) entityElementType.getAssociatedJoinable( sessionFactory() )
			);
		}
	}

	private void registerQuerySpaceAliases(String querySpaceUid, EntityReferenceAliases entityReferenceAliases) {
		if ( entityReferenceAliasesMap == null ) {
			entityReferenceAliasesMap = new HashMap<String, EntityReferenceAliases>();
		}
		entityReferenceAliasesMap.put( querySpaceUid, entityReferenceAliases );
		registerSqlTableAliasMapping( querySpaceUid, entityReferenceAliases.getTableAlias() );
	}

	private void registerSqlTableAliasMapping(String querySpaceUid, String sqlTableAlias) {
		if ( querySpaceUidToSqlTableAliasMap == null ) {
			querySpaceUidToSqlTableAliasMap = new HashMap<String, String>();
		}
		String old = querySpaceUidToSqlTableAliasMap.put( safeInterning( querySpaceUid ), safeInterning( sqlTableAlias ) );
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

	private void registerQuerySpaceAliases(String querySpaceUid, CollectionReferenceAliases collectionReferenceAliases) {
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
							+ String.join( ", ", entityAliases.getColumnAliases().getSuffixedKeyAliases() )
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
							+ String.join( ", ", collectionReferenceAliases.getCollectionColumnAliases().getSuffixedKeyAliases() )
			);
			final EntityReferenceAliases elementAliases = collectionReferenceAliases.getEntityElementAliases();
			if ( elementAliases != null ) {
				printWriter.println(
						TreePrinterHelper.INSTANCE.generateNodePrefix( depth+3 )
								+ "entity-element alias suffix - " + elementAliases.getColumnAliases().getSuffix()
				);
				printWriter.println(
						TreePrinterHelper.INSTANCE.generateNodePrefix( depth+3 )
								+ elementAliases.getColumnAliases().getSuffix()
								+ "entity-element suffixed key columns - "
								+ String.join( ", ", elementAliases.getColumnAliases().getSuffixedKeyAliases() )
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

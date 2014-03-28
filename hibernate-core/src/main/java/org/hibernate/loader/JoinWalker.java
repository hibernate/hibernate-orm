/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.loader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.hibernate.FetchMode;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.MappingException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.internal.JoinHelper;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.persister.entity.Loadable;
import org.hibernate.persister.entity.OuterJoinLoadable;
import org.hibernate.sql.ConditionFragment;
import org.hibernate.sql.DisjunctionFragment;
import org.hibernate.sql.InFragment;
import org.hibernate.sql.JoinFragment;
import org.hibernate.sql.JoinType;
import org.hibernate.type.AssociationType;
import org.hibernate.type.CompositeType;
import org.hibernate.type.EntityType;
import org.hibernate.type.ForeignKeyDirection;
import org.hibernate.type.Type;

/**
 * Walks the metamodel, searching for joins, and collecting
 * together information needed by <tt>OuterJoinLoader</tt>.
 * 
 * @see OuterJoinLoader
 * @author Gavin King, Jon Lipsky
 */
public class JoinWalker {
	
	private final SessionFactoryImplementor factory;
	protected final List associations = new ArrayList();
	private final Set visitedAssociationKeys = new HashSet();
	private final LoadQueryInfluencers loadQueryInfluencers;

	protected String[] suffixes;
	protected String[] collectionSuffixes;
	protected Loadable[] persisters;
	protected int[] owners;
	protected EntityType[] ownerAssociationTypes;
	protected CollectionPersister[] collectionPersisters;
	protected int[] collectionOwners;
	protected String[] aliases;
	protected LockOptions lockOptions;
	protected LockMode[] lockModeArray;
	protected String sql;

	protected JoinWalker(
			SessionFactoryImplementor factory,
			LoadQueryInfluencers loadQueryInfluencers) {
		this.factory = factory;
		this.loadQueryInfluencers = loadQueryInfluencers;

	}

	public List getAssociations() {
		return Collections.unmodifiableList( associations );
	}

	public String[] getCollectionSuffixes() {
		return collectionSuffixes;
	}

	public void setCollectionSuffixes(String[] collectionSuffixes) {
		this.collectionSuffixes = collectionSuffixes;
	}

	public LockOptions getLockModeOptions() {
		return lockOptions;
	}

	public LockMode[] getLockModeArray() {
		return lockModeArray;
	}

	public String[] getSuffixes() {
		return suffixes;
	}

	public void setSuffixes(String[] suffixes) {
		this.suffixes = suffixes;
	}

	public String[] getAliases() {
		return aliases;
	}

	public void setAliases(String[] aliases) {
		this.aliases = aliases;
	}

	public int[] getCollectionOwners() {
		return collectionOwners;
	}

	public void setCollectionOwners(int[] collectionOwners) {
		this.collectionOwners = collectionOwners;
	}

	public CollectionPersister[] getCollectionPersisters() {
		return collectionPersisters;
	}

	public void setCollectionPersisters(CollectionPersister[] collectionPersisters) {
		this.collectionPersisters = collectionPersisters;
	}

	public EntityType[] getOwnerAssociationTypes() {
		return ownerAssociationTypes;
	}

	public void setOwnerAssociationTypes(EntityType[] ownerAssociationType) {
		this.ownerAssociationTypes = ownerAssociationType;
	}

	public int[] getOwners() {
		return owners;
	}

	public void setOwners(int[] owners) {
		this.owners = owners;
	}

	public Loadable[] getPersisters() {
		return persisters;
	}

	public void setPersisters(Loadable[] persisters) {
		this.persisters = persisters;
	}

	public String getSQLString() {
		return sql;
	}

	public void setSql(String sql) {
		this.sql = sql;
	}

	protected SessionFactoryImplementor getFactory() {
		return factory;
	}

	protected Dialect getDialect() {
		return factory.getDialect();
	}

	public LoadQueryInfluencers getLoadQueryInfluencers() {
		return loadQueryInfluencers;
	}

	/**
	 * Add on association (one-to-one, many-to-one, or a collection) to a list 
	 * of associations to be fetched by outerjoin (if necessary)
	 */
	private void addAssociationToJoinTreeIfNecessary(
			final AssociationType type,
			final String[] aliasedLhsColumns,
			final String alias,
			final PropertyPath path,
			int currentDepth,
			final JoinType joinType) throws MappingException {
		if ( joinType != JoinType.NONE ) {
			addAssociationToJoinTree(
					type, 
					aliasedLhsColumns, 
					alias, 
					path,
					currentDepth,
					joinType
			);
		}
	}

	protected boolean hasRestriction(PropertyPath path)	{
		return false;
	}

	protected String getWithClause(PropertyPath path)	{
		return "";
	}
	
	/**
	 * Add on association (one-to-one, many-to-one, or a collection) to a list 
	 * of associations to be fetched by outerjoin 
	 */
	private void addAssociationToJoinTree(
			final AssociationType type,
			final String[] aliasedLhsColumns,
			final String alias,
			final PropertyPath path,
			final int currentDepth,
			final JoinType joinType) throws MappingException {

		Joinable joinable = type.getAssociatedJoinable( getFactory() );

		// important to generate alias based on size of association collection
		// *before* adding this join to that collection
		String subalias = generateTableAlias( associations.size() + 1, path, joinable );

		// NOTE : it should be fine to continue to pass only filters below
		// (instead of LoadQueryInfluencers) since "from that point on" we
		// only need to worry about restrictions (and not say adding more
		// joins)
		OuterJoinableAssociation assoc = new OuterJoinableAssociation(
				path,
				type, 
				alias, 
				aliasedLhsColumns, 
				subalias, 
				joinType, 
				getWithClause(path),
				hasRestriction( path ),
				getFactory(),
				loadQueryInfluencers.getEnabledFilters()
		);
		assoc.validateJoin( path.getFullPath() );
		associations.add( assoc );

		int nextDepth = currentDepth + 1;
//		path = "";
		if ( !joinable.isCollection() ) {
			if (joinable instanceof OuterJoinLoadable) {
				walkEntityTree(
					(OuterJoinLoadable) joinable, 
					subalias,
					path, 
					nextDepth
				);
			}
		}
		else {
			if (joinable instanceof QueryableCollection) {
				walkCollectionTree(
					(QueryableCollection) joinable, 
					subalias, 
					path, 
					nextDepth
				);
			}
		}

	}

	/**
	 * Walk the association tree for an entity, adding associations which should
	 * be join fetched to the {@link #associations} inst var.  This form is the
	 * entry point into the walking for a given entity, starting the recursive
	 * calls into {@link #walkEntityTree(org.hibernate.persister.entity.OuterJoinLoadable, String, PropertyPath ,int)}.
	 *
	 * @param persister The persister representing the entity to be walked.
	 * @param alias The (root) alias to use for this entity/persister.
	 * @throws org.hibernate.MappingException ???
	 */
	protected final void walkEntityTree(
			OuterJoinLoadable persister,
			String alias) throws MappingException {
		walkEntityTree( persister, alias, new PropertyPath(), 0 );
	}

	/**
	 * For a collection role, return a list of associations to be fetched by outerjoin
	 */
	protected final void walkCollectionTree(QueryableCollection persister, String alias) throws MappingException {
		walkCollectionTree( persister, alias, new PropertyPath(), 0 );
		//TODO: when this is the entry point, we should use an INNER_JOIN for fetching the many-to-many elements!
	}

	/**
	 * For a collection role, return a list of associations to be fetched by outerjoin
	 */
	private void walkCollectionTree(
			final QueryableCollection persister,
			final String alias,
			final PropertyPath path,
			final int currentDepth)	throws MappingException {

		if ( persister.isOneToMany() ) {
			walkEntityTree(
					(OuterJoinLoadable) persister.getElementPersister(),
					alias,
					path,
					currentDepth
				);
		}
		else {
			Type type = persister.getElementType();
			if ( type.isAssociationType() ) {
				// a many-to-many;
				// decrement currentDepth here to allow join across the association table
				// without exceeding MAX_FETCH_DEPTH (i.e. the "currentDepth - 1" bit)
				AssociationType associationType = (AssociationType) type;
				String[] aliasedLhsColumns = persister.getElementColumnNames(alias);
				String[] lhsColumns = persister.getElementColumnNames();
				// if the current depth is 0, the root thing being loaded is the
				// many-to-many collection itself.  Here, it is alright to use
				// an inner join...
				boolean useInnerJoin = currentDepth == 0;
				final JoinType joinType = getJoinType(
						associationType,
						persister.getFetchMode(),
						path,
						persister.getTableName(),
						lhsColumns,
						!useInnerJoin,
						currentDepth - 1, 
						null //operations which cascade as far as the collection also cascade to collection elements
				);
				addAssociationToJoinTreeIfNecessary(
						associationType,
						aliasedLhsColumns,
						alias,
						path,
						currentDepth - 1,
						joinType
					);
			}
			else if ( type.isComponentType() ) {
				walkCompositeElementTree(
						(CompositeType) type,
						persister.getElementColumnNames(),
						persister,
						alias,
						path,
						currentDepth
					);
			}
		}

	}
	
	/**
	 * Process a particular association owned by the entity
	 *
	 * @param associationType The type representing the association to be
	 * processed.
	 * @param persister The owner of the association to be processed.
	 * @param propertyNumber The property number for the association
	 * (relative to the persister).
	 * @param alias The entity alias
	 * @param path The path to the association
	 * @param nullable is the association nullable (which I think is supposed
	 * to indicate inner/outer join semantics).
	 * @param currentDepth The current join depth
	 * @throws org.hibernate.MappingException ???
	 */
	private void walkEntityAssociationTree(
			final AssociationType associationType,
			final OuterJoinLoadable persister,
			final int propertyNumber,
			final String alias,
			final PropertyPath path,
			final boolean nullable,
			final int currentDepth) throws MappingException {
		String[] aliasedLhsColumns = JoinHelper.getAliasedLHSColumnNames(
				associationType, alias, propertyNumber, persister, getFactory()
		);
		String[] lhsColumns = JoinHelper.getLHSColumnNames(
				associationType, propertyNumber, persister, getFactory()
		);
		String lhsTable = JoinHelper.getLHSTableName(associationType, propertyNumber, persister);

		PropertyPath subPath = path.append( persister.getSubclassPropertyName(propertyNumber) );
		JoinType joinType = getJoinType(
				persister,
				subPath,
				propertyNumber,
				associationType,
				persister.getFetchMode( propertyNumber ),
				persister.getCascadeStyle( propertyNumber ),
				lhsTable,
				lhsColumns,
				nullable,
				currentDepth
		);
		addAssociationToJoinTreeIfNecessary(
				associationType,
				aliasedLhsColumns,
				alias,
				subPath,
				currentDepth,
				joinType
		);
	}

	/**
	 * Determine the appropriate type of join (if any) to use to fetch the
	 * given association.
	 *
	 * @param persister The owner of the association.
	 * @param path The path to the association
	 * @param propertyNumber The property number representing the association.
	 * @param associationType The association type.
	 * @param metadataFetchMode The metadata-defined fetch mode.
	 * @param metadataCascadeStyle The metadata-defined cascade style.
	 * @param lhsTable The owner table
	 * @param lhsColumns The owner join columns
	 * @param nullable Is the association nullable.
	 * @param currentDepth Current join depth
	 * @return type of join to use ({@link org.hibernate.sql.JoinType#INNER_JOIN},
	 * {@link org.hibernate.sql.JoinType#LEFT_OUTER_JOIN}, or -1 to indicate no joining.
	 * @throws MappingException ??
	 */
	protected JoinType getJoinType(
			OuterJoinLoadable persister,
			final PropertyPath path,
			int propertyNumber,
			AssociationType associationType,
			FetchMode metadataFetchMode,
			CascadeStyle metadataCascadeStyle,
			String lhsTable,
			String[] lhsColumns,
			final boolean nullable,
			final int currentDepth) throws MappingException {
		return getJoinType(
				associationType,
				metadataFetchMode,
				path,
				lhsTable,
				lhsColumns,
				nullable,
				currentDepth,
				metadataCascadeStyle
		);
	}

	/**
	 * Determine the appropriate associationType of join (if any) to use to fetch the
	 * given association.
	 *
	 * @param associationType The association associationType.
	 * @param config The metadata-defined fetch mode.
	 * @param path The path to the association
	 * @param lhsTable The owner table
	 * @param lhsColumns The owner join columns
	 * @param nullable Is the association nullable.
	 * @param currentDepth Current join depth
	 * @param cascadeStyle The metadata-defined cascade style.
	 * @return type of join to use ({@link org.hibernate.sql.JoinType#INNER_JOIN},
	 * {@link org.hibernate.sql.JoinType#LEFT_OUTER_JOIN}, or -1 to indicate no joining.
	 * @throws MappingException ??
	 */
	protected JoinType getJoinType(
			AssociationType associationType,
			FetchMode config,
			PropertyPath path,
			String lhsTable,
			String[] lhsColumns,
			boolean nullable,
			int currentDepth,
			CascadeStyle cascadeStyle) throws MappingException {
		if  ( !isJoinedFetchEnabled( associationType, config, cascadeStyle ) ) {
			return JoinType.NONE;
		}
		if ( isTooDeep(currentDepth) || ( associationType.isCollectionType() && isTooManyCollections() ) ) {
			return JoinType.NONE;
		}
		if ( isDuplicateAssociation( lhsTable, lhsColumns, associationType ) ) {
			return JoinType.NONE;
		}
		return getJoinType( nullable, currentDepth );
	}

	/**
	 * Walk the association tree for an entity, adding associations which should
	 * be join fetched to the {@link #associations} inst var.  This form is the
	 * entry point into the walking for a given entity, starting the recursive
	 * calls into {@link #walkEntityTree(org.hibernate.persister.entity.OuterJoinLoadable, String, PropertyPath ,int)}.
	 *
	 * @param persister The persister representing the entity to be walked.
	 * @param alias The (root) alias to use for this entity/persister.
	 * @param path The property path to the entity being walked
	 * @param currentDepth The current join depth
	 * @throws org.hibernate.MappingException ???
	 */
	private void walkEntityTree(
			final OuterJoinLoadable persister,
			final String alias,
			final PropertyPath path,
			final int currentDepth) throws MappingException {
		int n = persister.countSubclassProperties();
		for ( int i = 0; i < n; i++ ) {
			Type type = persister.getSubclassPropertyType(i);
			if ( type.isAssociationType() ) {
				walkEntityAssociationTree(
					( AssociationType ) type,
					persister,
					i,
					alias,
					path,
					persister.isSubclassPropertyNullable(i),
					currentDepth
				);
			}
			else if ( type.isComponentType() ) {
				walkComponentTree(
						( CompositeType ) type,
						i,
						0,
						persister,
						alias,
						path.append( persister.getSubclassPropertyName(i) ),
						currentDepth
				);
			}
		}

		// if the entity has a composite identifier, see if we need to handle
		// its sub-properties separately
		final Type idType = persister.getIdentifierType();
		if ( idType.isComponentType() ) {
			final CompositeType cidType = (CompositeType) idType;
			if ( persister.getEntityMetamodel().getIdentifierProperty().isVirtual() ) {
				// we have a non-aggregated composite identifier.  We need to process
				// the composite sub-properties separately
				if ( persister.getEntityMetamodel().getIdentifierProperty().isEmbedded() ) {
					walkComponentTree(
							cidType,
							-1,
							0,
							persister,
							alias,
							path,
							currentDepth
					);
				}
			}
//			if ( cidType.isEmbedded() ) {
//				// we have an embedded composite identifier.  Most likely we need to process the composite
//				// properties separately, although there is an edge case where the identifier is really
//				// a simple identifier (single value) wrapped in a JPA @IdClass or even in the case of a
//				// a simple identifier (single value) wrapped in a Hibernate composite type.
//				//
//				// We really do not have a built-in method to determine that.  However, generally the
//				// persister would report that there is single, physical identifier property which is
//				// explicitly at odds with the notion of "embedded composite".  So we use that for now
//				if ( persister.getEntityMetamodel().getIdentifierProperty().isEmbedded() ) {
//					walkComponentTree(
//							cidType,
//							-1,
//							0,
//							persister,
//							alias,
//							path,
//							currentDepth
//					);
//				}
//			}
		}
	}

	/**
	 * For a component, add to a list of associations to be fetched by outerjoin
	 *
	 *
	 * @param componentType The component type to be walked.
	 * @param propertyNumber The property number for the component property (relative to
	 * persister).
	 * @param begin todo unknowm
	 * @param persister The owner of the component property
	 * @param alias The root alias
	 * @param path The property access path
	 * @param currentDepth The current join depth
	 * @throws org.hibernate.MappingException ???
	 */
	private void walkComponentTree(
			final CompositeType componentType,
			final int propertyNumber,
			int begin,
			final OuterJoinLoadable persister,
			final String alias,
			final PropertyPath path,
			final int currentDepth) throws MappingException {
		Type[] types = componentType.getSubtypes();
		String[] propertyNames = componentType.getPropertyNames();
		for ( int i = 0; i < types.length; i++ ) {
			if ( types[i].isAssociationType() ) {
				AssociationType associationType = (AssociationType) types[i];
				String[] aliasedLhsColumns = JoinHelper.getAliasedLHSColumnNames(
					associationType, alias, propertyNumber, begin, persister, getFactory()
				);
				String[] lhsColumns = JoinHelper.getLHSColumnNames(
					associationType, propertyNumber, begin, persister, getFactory()
				);
				String lhsTable = JoinHelper.getLHSTableName(associationType, propertyNumber, persister);

				final PropertyPath subPath = path.append( propertyNames[i] );
				final boolean[] propertyNullability = componentType.getPropertyNullability();
				final JoinType joinType = getJoinType(
						persister,
						subPath,
						propertyNumber,
						associationType,
						componentType.getFetchMode(i),
						componentType.getCascadeStyle(i),
						lhsTable,
						lhsColumns,
						propertyNullability==null || propertyNullability[i],
						currentDepth
				);
				addAssociationToJoinTreeIfNecessary(			
						associationType,
						aliasedLhsColumns,
						alias,
						subPath,
						currentDepth,
						joinType
				);

			}
			else if ( types[i].isComponentType() ) {
				final PropertyPath subPath = path.append( propertyNames[i] );
				walkComponentTree(
						( CompositeType ) types[i],
						propertyNumber,
						begin,
						persister,
						alias,
						subPath,
						currentDepth
				);
			}
			begin += types[i].getColumnSpan( getFactory() );
		}

	}

	/**
	 * For a composite element, add to a list of associations to be fetched by outerjoin
	 */
	private void walkCompositeElementTree(
			final CompositeType compositeType,
			final String[] cols,
			final QueryableCollection persister,
			final String alias,
			final PropertyPath path,
			final int currentDepth) throws MappingException {

		Type[] types = compositeType.getSubtypes();
		String[] propertyNames = compositeType.getPropertyNames();
		int begin = 0;
		for ( int i=0; i <types.length; i++ ) {
			int length = types[i].getColumnSpan( getFactory() );
			String[] lhsColumns = ArrayHelper.slice(cols, begin, length);

			if ( types[i].isAssociationType() ) {
				AssociationType associationType = (AssociationType) types[i];

				// simple, because we can't have a one-to-one or a collection 
				// (or even a property-ref) in a composite-element:
				String[] aliasedLhsColumns = StringHelper.qualify(alias, lhsColumns);

				final PropertyPath subPath = path.append( propertyNames[i] );
				final boolean[] propertyNullability = compositeType.getPropertyNullability();
				final JoinType joinType = getJoinType(
						associationType,
						compositeType.getFetchMode(i),
						subPath,
						persister.getTableName(),
						lhsColumns,
						propertyNullability==null || propertyNullability[i],
						currentDepth, 
						compositeType.getCascadeStyle(i)
					);
				addAssociationToJoinTreeIfNecessary(
						associationType,
						aliasedLhsColumns,
						alias,
						subPath,
						currentDepth,
						joinType
					);
			}
			else if ( types[i].isComponentType() ) {
				final PropertyPath subPath = path.append( propertyNames[i] );
				walkCompositeElementTree(
						(CompositeType) types[i],
						lhsColumns,
						persister,
						alias,
						subPath,
						currentDepth
					);
			}
			begin+=length;
		}

	}

	/**
	 * Use an inner join if it is a non-null association and this
	 * is the "first" join in a series
	 */
	protected JoinType getJoinType(boolean nullable, int currentDepth) {
		//TODO: this is too conservative; if all preceding joins were 
		//      also inner joins, we could use an inner join here
		//
		// IMPL NOTE : currentDepth might be less-than zero if this is the
		// 		root of a many-to-many collection initializer 
		return !nullable && currentDepth <= 0
				? JoinType.INNER_JOIN
				: JoinType.LEFT_OUTER_JOIN;
	}

	protected boolean isTooDeep(int currentDepth) {
		Integer maxFetchDepth = getFactory().getSettings().getMaximumFetchDepth();
		return maxFetchDepth!=null && currentDepth >= maxFetchDepth;
	}
	
	protected boolean isTooManyCollections() {
		return false;
	}
	
	/**
	 * Does the mapping, and Hibernate default semantics, specify that
	 * this association should be fetched by outer joining
	 */
	protected boolean isJoinedFetchEnabledInMapping(FetchMode config, AssociationType type) 
	throws MappingException {
		if ( !type.isEntityType() && !type.isCollectionType() ) {
			return false;
		}
		else {
			if (config==FetchMode.JOIN) return true;
			if (config==FetchMode.SELECT) return false;
			if ( type.isEntityType() ) {
				//TODO: look at the owning property and check that it 
				//      isn't lazy (by instrumentation)
				EntityType entityType =(EntityType) type;
				EntityPersister persister = getFactory().getEntityPersister( entityType.getAssociatedEntityName() );
				return !persister.hasProxy();
			}
			else {
				return false;
			}
		}
	}

	/**
	 * Override on subclasses to enable or suppress joining 
	 * of certain association types
	 */
	protected boolean isJoinedFetchEnabled(AssociationType type, FetchMode config, CascadeStyle cascadeStyle) {
		return type.isEntityType() && isJoinedFetchEnabledInMapping(config, type) ;
	}
	
	protected String generateTableAlias(final int n, final PropertyPath path, final Joinable joinable) {
		return StringHelper.generateAlias( joinable.getName(), n );
	}

	protected String generateRootAlias(final String description) {
		return StringHelper.generateAlias(description, 0);
	}

	/**
	 * Used to detect circularities in the joined graph, note that 
	 * this method is side-effecty
	 */
	protected boolean isDuplicateAssociation(final String foreignKeyTable, final String[] foreignKeyColumns) {
		AssociationKey associationKey = new AssociationKey(foreignKeyColumns, foreignKeyTable);
		return !visitedAssociationKeys.add( associationKey );
	}
	
	/**
	 * Used to detect circularities in the joined graph, note that 
	 * this method is side-effecty
	 */
	protected boolean isDuplicateAssociation(final String lhsTable, final String[] lhsColumnNames, final AssociationType type) {
		final String foreignKeyTable;
		final String[] foreignKeyColumns;
		if ( type.getForeignKeyDirection()==ForeignKeyDirection.FROM_PARENT ) {
			foreignKeyTable = lhsTable;
			foreignKeyColumns = lhsColumnNames;
		}
		else {
			foreignKeyTable = type.getAssociatedJoinable( getFactory() ).getTableName();
			foreignKeyColumns = JoinHelper.getRHSColumnNames( type, getFactory() );
		}
		return isDuplicateAssociation(foreignKeyTable, foreignKeyColumns);
	}
	
	/**
	 * Uniquely identifier a foreign key, so that we don't
	 * join it more than once, and create circularities
	 */
	private static final class AssociationKey {
		private String[] columns;
		private String table;
		private AssociationKey(String[] columns, String table) {
			this.columns = columns;
			this.table = table;
		}
		@Override
        public boolean equals(Object other) {
			AssociationKey that = (AssociationKey) other;
			return that.table.equals(table) && Arrays.equals(columns, that.columns);
		}
		@Override
        public int hashCode() {
			return table.hashCode(); //TODO: inefficient
		}
	}
	
	/**
	 * Should we join this association?
	 */
	protected boolean isJoinable(
			final JoinType joinType,
			final Set visitedAssociationKeys,
			final String lhsTable,
			final String[] lhsColumnNames,
			final AssociationType type,
			final int depth) {

		if ( joinType == JoinType.NONE ) {
			return false;
		}
		
		if ( joinType == JoinType.INNER_JOIN ) {
			return true;
		}

		Integer maxFetchDepth = getFactory().getSettings().getMaximumFetchDepth();
		final boolean tooDeep = maxFetchDepth!=null && depth >= maxFetchDepth;
		
		return !tooDeep && !isDuplicateAssociation(lhsTable, lhsColumnNames, type);
	}
	
	protected String orderBy(final List associations, final String orderBy) {
		return mergeOrderings( orderBy( associations ), orderBy );
	}

	protected static String mergeOrderings(String ordering1, String ordering2) {
		if ( ordering1.length() == 0 ) {
			return ordering2;
		}
		else if ( ordering2.length() == 0 ) {
			return ordering1;
		}
		else {
			return ordering1 + ", " + ordering2;
		}
	}
	
	/**
	 * Generate a sequence of <tt>LEFT OUTER JOIN</tt> clauses for the given associations.
	 */
	protected final JoinFragment mergeOuterJoins(List associations)
	throws MappingException {
		JoinFragment outerjoin = getDialect().createOuterJoinFragment();
		Iterator iter = associations.iterator();
		OuterJoinableAssociation last = null;
		while ( iter.hasNext() ) {
			OuterJoinableAssociation oj = (OuterJoinableAssociation) iter.next();
			if ( last != null && last.isManyToManyWith( oj ) ) {
				oj.addManyToManyJoin( outerjoin, ( QueryableCollection ) last.getJoinable() );
			}
			else {
				oj.addJoins(outerjoin);
			}
			last = oj;
		}
		last = null;
		return outerjoin;
	}

	/**
	 * Count the number of instances of Joinable which are actually
	 * also instances of Loadable, or are one-to-many associations
	 */
	protected static final int countEntityPersisters(List associations)
	throws MappingException {
		int result = 0;
		Iterator iter = associations.iterator();
		while ( iter.hasNext() ) {
			OuterJoinableAssociation oj = (OuterJoinableAssociation) iter.next();
			if ( oj.getJoinable().consumesEntityAlias() ) {
				result++;
			}
		}
		return result;
	}
	
	/**
	 * Count the number of instances of Joinable which are actually
	 * also instances of PersistentCollection which are being fetched
	 * by outer join
	 */
	protected static final int countCollectionPersisters(List associations)
	throws MappingException {
		int result = 0;
		Iterator iter = associations.iterator();
		while ( iter.hasNext() ) {
			OuterJoinableAssociation oj = (OuterJoinableAssociation) iter.next();
			if ( oj.getJoinType()==JoinType.LEFT_OUTER_JOIN &&
					oj.getJoinable().isCollection() &&
					! oj.hasRestriction() ) {
				result++;
			}
		}
		return result;
	}
	
	/**
	 * Get the order by string required for collection fetching
	 */
	protected static final String orderBy(List associations)
	throws MappingException {
		StringBuilder buf = new StringBuilder();
		Iterator iter = associations.iterator();
		OuterJoinableAssociation last = null;
		while ( iter.hasNext() ) {
			OuterJoinableAssociation oj = (OuterJoinableAssociation) iter.next();
			if ( oj.getJoinType() == JoinType.LEFT_OUTER_JOIN ) { // why does this matter?
				if ( oj.getJoinable().isCollection() ) {
					final QueryableCollection queryableCollection = (QueryableCollection) oj.getJoinable();
					if ( queryableCollection.hasOrdering() ) {
						final String orderByString = queryableCollection.getSQLOrderByString( oj.getRHSAlias() );
						buf.append( orderByString ).append(", ");
					}
				}
				else {
					// it might still need to apply a collection ordering based on a
					// many-to-many defined order-by...
					if ( last != null && last.getJoinable().isCollection() ) {
						final QueryableCollection queryableCollection = (QueryableCollection) last.getJoinable();
						if ( queryableCollection.isManyToMany() && last.isManyToManyWith( oj ) ) {
							if ( queryableCollection.hasManyToManyOrdering() ) {
								final String orderByString = queryableCollection.getManyToManyOrderByString( oj.getRHSAlias() );
								buf.append( orderByString ).append(", ");
							}
						}
					}
				}
			}
			last = oj;
		}
		if ( buf.length()>0 ) buf.setLength( buf.length()-2 );
		return buf.toString();
	}

	/**
	 * Render the where condition for a (batch) load by identifier / collection key
	 */
	protected StringBuilder whereString(String alias, String[] columnNames, int batchSize) {
		if ( columnNames.length==1 ) {
			// if not a composite key, use "foo in (?, ?, ?)" for batching
			// if no batch, and not a composite key, use "foo = ?"
			InFragment in = new InFragment().setColumn( alias, columnNames[0] );
			for ( int i=0; i<batchSize; i++ ) in.addValue("?");
			return new StringBuilder( in.toFragmentString() );
		}
		else {
			//a composite key
			ConditionFragment byId = new ConditionFragment()
					.setTableAlias(alias)
					.setCondition( columnNames, "?" );
	
			StringBuilder whereString = new StringBuilder();
			if ( batchSize==1 ) {
				// if no batch, use "foo = ? and bar = ?"
				whereString.append( byId.toFragmentString() );
			}
			else {
				// if a composite key, use "( (foo = ? and bar = ?) or (foo = ? and bar = ?) )" for batching
				whereString.append('('); //TODO: unnecessary for databases with ANSI-style joins
				DisjunctionFragment df = new DisjunctionFragment();
				for ( int i=0; i<batchSize; i++ ) {
					df.addCondition(byId);
				}
				whereString.append( df.toFragmentString() );
				whereString.append(')'); //TODO: unnecessary for databases with ANSI-style joins
			}
			return whereString;
		}
	}


	protected void initPersisters(final List associations, final LockMode lockMode) throws MappingException {
		initPersisters( associations, new LockOptions(lockMode));
	}

	protected static interface AssociationInitCallback {
		public static final AssociationInitCallback NO_CALLBACK = new AssociationInitCallback() {
			public void associationProcessed(OuterJoinableAssociation oja, int position) {
			}
		};

		public void associationProcessed(OuterJoinableAssociation oja, int position);
	}
	protected void initPersisters(final List associations, final LockOptions lockOptions) throws MappingException {
		initPersisters( associations, lockOptions, AssociationInitCallback.NO_CALLBACK );
	}

	protected void initPersisters(
			final List associations,
			final LockOptions lockOptions,
			final AssociationInitCallback callback) throws MappingException {
		final int joins = countEntityPersisters(associations);
		final int collections = countCollectionPersisters(associations);

		collectionOwners = collections==0 ? null : new int[collections];
		collectionPersisters = collections==0 ? null : new CollectionPersister[collections];
		collectionSuffixes = BasicLoader.generateSuffixes( joins + 1, collections );

		this.lockOptions = lockOptions;

		persisters = new Loadable[joins];
		aliases = new String[joins];
		owners = new int[joins];
		ownerAssociationTypes = new EntityType[joins];
		lockModeArray = ArrayHelper.fillArray( lockOptions.getLockMode(), joins );

		int i=0;
		int j=0;
		Iterator iter = associations.iterator();
		while ( iter.hasNext() ) {
			final OuterJoinableAssociation oj = (OuterJoinableAssociation) iter.next();
			if ( !oj.isCollection() ) {
				
				persisters[i] = (Loadable) oj.getJoinable();
				aliases[i] = oj.getRHSAlias();
				owners[i] = oj.getOwner(associations);
				ownerAssociationTypes[i] = (EntityType) oj.getJoinableType();
				callback.associationProcessed( oj, i );
				i++;
				
			}
			else {
				
				QueryableCollection collPersister = (QueryableCollection) oj.getJoinable();
				if ( oj.getJoinType()==JoinType.LEFT_OUTER_JOIN && ! oj.hasRestriction() ) {
					//it must be a collection fetch
					collectionPersisters[j] = collPersister;
					collectionOwners[j] = oj.getOwner(associations);
					j++;
				}
	
				if ( collPersister.isOneToMany() ) {
					persisters[i] = (Loadable) collPersister.getElementPersister();
					aliases[i] = oj.getRHSAlias();
					callback.associationProcessed( oj, i );
					i++;
				}
			}
		}

		if ( ArrayHelper.isAllNegative(owners) ) owners = null;
		if ( collectionOwners!=null && ArrayHelper.isAllNegative(collectionOwners) ) {
			collectionOwners = null;
		}
	}

	/**
	 * Generate a select list of columns containing all properties of the entity classes
	 */
	protected final String selectString(List associations)
	throws MappingException {

		if ( associations.size()==0 ) {
			return "";
		}
		else {
			StringBuilder buf = new StringBuilder( associations.size() * 100 );
			int entityAliasCount=0;
			int collectionAliasCount=0;
			for ( int i=0; i<associations.size(); i++ ) {
				OuterJoinableAssociation join = (OuterJoinableAssociation) associations.get(i);
				OuterJoinableAssociation next = (i == associations.size() - 1)
				        ? null
				        : ( OuterJoinableAssociation ) associations.get( i + 1 );
				final Joinable joinable = join.getJoinable();
				final String entitySuffix = ( suffixes == null || entityAliasCount >= suffixes.length )
				        ? null
				        : suffixes[entityAliasCount];
				final String collectionSuffix = ( collectionSuffixes == null || collectionAliasCount >= collectionSuffixes.length )
				        ? null
				        : collectionSuffixes[collectionAliasCount];
				final String selectFragment = joinable.selectFragment(
						next == null ? null : next.getJoinable(),
						next == null ? null : next.getRHSAlias(),
						join.getRHSAlias(),
						entitySuffix,
				        collectionSuffix,
						join.getJoinType()==JoinType.LEFT_OUTER_JOIN
				);
				if (selectFragment.trim().length() > 0) {
					buf.append(", ").append(selectFragment);
				}
				if ( joinable.consumesEntityAlias() ) entityAliasCount++;
				if ( joinable.consumesCollectionAlias() && join.getJoinType()==JoinType.LEFT_OUTER_JOIN ) collectionAliasCount++;
			}
			return buf.toString();
		}
	}

}

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.ast.tree;

import org.hibernate.QueryException;
import org.hibernate.engine.internal.JoinSequence;
import org.hibernate.hql.internal.CollectionProperties;
import org.hibernate.hql.internal.antlr.SqlTokenTypes;
import org.hibernate.hql.internal.ast.util.ASTUtil;
import org.hibernate.hql.internal.ast.util.ColumnHelper;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.log.DeprecationLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.loader.plan.spi.EntityQuerySpace;
import org.hibernate.loader.plan.spi.QuerySpace;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.sql.JoinType;
import org.hibernate.type.CollectionType;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;

import antlr.SemanticException;
import antlr.collections.AST;

/**
 * Represents a reference to a property or alias expression.  This should duplicate the relevant behaviors in
 * PathExpressionParser.
 *
 * @author Joshua Davis
 */
public class DotNode extends FromReferenceNode implements DisplayableNode, SelectExpression, TableReferenceNode {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( DotNode.class );

	///////////////////////////////////////////////////////////////////////////
	// USED ONLY FOR REGRESSION TESTING!!!!
	//
	// todo : obviously get rid of all this junk ;)
	///////////////////////////////////////////////////////////////////////////
	public static boolean useThetaStyleImplicitJoins;
	public static boolean regressionStyleJoinSuppression;

	public interface IllegalCollectionDereferenceExceptionBuilder {
		QueryException buildIllegalCollectionDereferenceException(
				String collectionPropertyName,
				FromReferenceNode lhs);
	}

	public static final IllegalCollectionDereferenceExceptionBuilder DEF_ILLEGAL_COLL_DEREF_EXCP_BUILDER = new IllegalCollectionDereferenceExceptionBuilder() {
		public QueryException buildIllegalCollectionDereferenceException(String propertyName, FromReferenceNode lhs) {
			String lhsPath = ASTUtil.getPathText( lhs );
			return new QueryException( "illegal attempt to dereference collection [" + lhsPath + "] with element property reference [" + propertyName + "]" );
		}
	};
	public static IllegalCollectionDereferenceExceptionBuilder ILLEGAL_COLL_DEREF_EXCP_BUILDER = DEF_ILLEGAL_COLL_DEREF_EXCP_BUILDER;
	///////////////////////////////////////////////////////////////////////////

	public static enum DereferenceType {
		UNKNOWN,
		ENTITY,
		COMPONENT,
		COLLECTION,
		PRIMITIVE,
		IDENTIFIER,
		JAVA_CONSTANT
	}

	/**
	 * The identifier that is the name of the property.
	 */
	private String propertyName;

	/**
	 * The full path, to the root alias of this dot node.
	 */
	private String path;

	/**
	 * The unresolved property path relative to this dot node.
	 */
	private String propertyPath;

	/**
	 * The column names that this resolves to.
	 */
	private String[] columns;

	/**
	 * The type of join to create.   Default is an inner join.
	 */
	private JoinType joinType = JoinType.INNER_JOIN;

	/**
	 * Fetch join or not.
	 */
	private boolean fetch;

	/**
	 * The type of dereference that happened
	 */
	private DereferenceType dereferenceType = DereferenceType.UNKNOWN;

	private FromElement impliedJoin;

	/**
	 * Sets the join type for this '.' node structure.
	 *
	 * @param joinType The type of join to use.
	 *
	 * @see org.hibernate.sql.JoinFragment
	 */
	public void setJoinType(JoinType joinType) {
		this.joinType = joinType;
	}

	private String[] getColumns() throws QueryException {
		if ( columns == null ) {
			// Use the table fromElement and the property name to get the array of column names.
			String tableAlias = getLhs().getFromElement().getTableAlias();
			columns = getFromElement().toColumns( tableAlias, propertyPath, false );
		}
		return columns;
	}

	@Override
	public String getDisplayText() {
		StringBuilder buf = new StringBuilder();
		FromElement fromElement = getFromElement();
		buf.append( "{propertyName=" ).append( propertyName );
		buf.append( ",dereferenceType=" ).append( dereferenceType.name() );
		buf.append( ",getPropertyPath=" ).append( propertyPath );
		buf.append( ",path=" ).append( getPath() );
		if ( fromElement != null ) {
			buf.append( ",tableAlias=" ).append( fromElement.getTableAlias() );
			buf.append( ",className=" ).append( fromElement.getClassName() );
			buf.append( ",classAlias=" ).append( fromElement.getClassAlias() );
		}
		else {
			buf.append( ",no from element" );
		}
		buf.append( '}' );
		return buf.toString();
	}

	/**
	 * Resolves the left hand side of the DOT.
	 *
	 * @throws SemanticException
	 */
	@Override
	public void resolveFirstChild() throws SemanticException {
		FromReferenceNode lhs = (FromReferenceNode) getFirstChild();
		SqlNode property = (SqlNode) lhs.getNextSibling();

		// Set the attributes of the property reference expression.
		String propName = property.getText();
		propertyName = propName;
		// If the uresolved property path isn't set yet, just use the property name.
		if ( propertyPath == null ) {
			propertyPath = propName;
		}
		// Resolve the LHS fully, generate implicit joins.  Pass in the property name so that the resolver can
		// discover foreign key (id) properties.
		lhs.resolve( true, true, null, this );
		setFromElement( lhs.getFromElement() );            // The 'from element' that the property is in.

		checkSubclassOrSuperclassPropertyReference( lhs, propName );
	}

	@Override
	public void resolveInFunctionCall(boolean generateJoin, boolean implicitJoin) throws SemanticException {
		if ( isResolved() ) {
			return;
		}
		Type propertyType = prepareLhs();            // Prepare the left hand side and get the data type.
		if ( propertyType != null && propertyType.isCollectionType() ) {
			resolveIndex( null );
		}
		else {
			resolveFirstChild();
			super.resolve( generateJoin, implicitJoin );
		}
	}


	public void resolveIndex(AST parent) throws SemanticException {
		if ( isResolved() ) {
			return;
		}
		Type propertyType = prepareLhs();            // Prepare the left hand side and get the data type.
		dereferenceCollection( (CollectionType) propertyType, true, true, null, parent );
	}

	public void resolve(boolean generateJoin, boolean implicitJoin, String classAlias, AST parent, AST parentPredicate)
			throws SemanticException {
		// If this dot has already been resolved, stop now.
		if ( isResolved() ) {
			return;
		}

		Type propertyType = prepareLhs(); // Prepare the left hand side and get the data type.

		if ( parent == null && AbstractEntityPersister.ENTITY_CLASS.equals( propertyName ) ) {
			DeprecationLogger.DEPRECATION_LOGGER.logDeprecationOfClassEntityTypeSelector( getLhs().getPath() );
		}

		// If there is no data type for this node, and we're at the end of the path (top most dot node), then
		// this might be a Java constant.
		if ( propertyType == null ) {
			if ( parent == null ) {
				getWalker().getLiteralProcessor().lookupConstant( this );
			}
			// If the propertyType is null and there isn't a parent, just
			// stop now... there was a problem resolving the node anyway.
			return;
		}

		if ( propertyType.isComponentType() ) {
			// The property is a component...
			checkLhsIsNotCollection();
			dereferenceComponent( parent );
			initText();
		}
		else if ( propertyType.isEntityType() ) {
			// The property is another class..
			checkLhsIsNotCollection();
			dereferenceEntity( (EntityType) propertyType, implicitJoin, classAlias, generateJoin, parent, parentPredicate );
			initText();
		}
		else if ( propertyType.isCollectionType() ) {
			// The property is a collection...
			checkLhsIsNotCollection();
			dereferenceCollection( (CollectionType) propertyType, implicitJoin, false, classAlias, parent );
		}
		else {
			// Otherwise, this is a primitive type.
			if ( !CollectionProperties.isAnyCollectionProperty( propertyName ) ) {
				checkLhsIsNotCollection();
			}
			dereferenceType = DereferenceType.PRIMITIVE;
			initText();
		}
		setResolved();
	}

	private void initText() {
		String[] cols = getColumns();
		String text = String.join( ", ", cols );
		boolean countDistinct = getWalker().isInCountDistinct()
				&& getWalker().getSessionFactoryHelper().getFactory().getDialect().requiresParensForTupleDistinctCounts();
		if ( cols.length > 1 &&
				( getWalker().isComparativeExpressionClause() || countDistinct ) ) {
			text = "(" + text + ")";
		}
		setText( text );
	}

	private Type prepareLhs() throws SemanticException {
		FromReferenceNode lhs = getLhs();
		lhs.prepareForDot( propertyName );
		return getDataType();
	}

	private void dereferenceCollection(
			CollectionType collectionType,
			boolean implicitJoin,
			boolean indexed,
			String classAlias,
			AST parent)
			throws SemanticException {

		dereferenceType = DereferenceType.COLLECTION;
		String role = collectionType.getRole();

		//foo.bars.size (also handles deprecated stuff like foo.bars.maxelement for backwardness)
		boolean isSizeProperty = getNextSibling() != null &&
				CollectionProperties.isAnyCollectionProperty( getNextSibling().getText() );

		if ( isSizeProperty ) {
			indexed = true; //yuck!
		}

		QueryableCollection queryableCollection = getSessionFactoryHelper().requireQueryableCollection( role );
		String propName = getPath();
		FromClause currentFromClause = getWalker().getCurrentFromClause();

		// If the lhs of the join is a "component join", we need to go back to the
		// first non-component-join as the origin to properly link aliases and
		// join columns
		FromElement lhsFromElement = getLhs().getFromElement();
		while ( lhsFromElement != null && ComponentJoin.class.isInstance( lhsFromElement ) ) {
			lhsFromElement = lhsFromElement.getOrigin();
		}
		if ( lhsFromElement == null ) {
			throw new QueryException( "Unable to locate appropriate lhs" );
		}

		// determine whether we should use the table name or table alias to qualify the column names...
		// we need to use the table-name when:
		//		1) the top-level statement is not a SELECT
		//		2) the LHS FromElement is *the* FromElement from the top-level statement
		//
		// there is a caveat here.. if the update/delete statement are "multi-table" we should continue to use
		// the alias also, even if the FromElement is the root one...
		//
		// in all other cases, we should use the table alias
		if ( getWalker().getStatementType() != SqlTokenTypes.SELECT ) {
			if ( isFromElementUpdateOrDeleteRoot( lhsFromElement ) ) {
				// at this point we know we have the 2 conditions above,
				// lets see if we have the mentioned "multi-table" caveat...
				boolean useAlias = false;
				if ( getWalker().getStatementType() != SqlTokenTypes.INSERT ) {
					final Queryable persister = lhsFromElement.getQueryable();
					if ( persister.isMultiTable() ) {
						useAlias = true;
					}
				}
				if ( !useAlias ) {
					final String lhsTableName = lhsFromElement.getQueryable().getTableName();
					columns = getFromElement().toColumns( lhsTableName, propertyPath, false, true );
				}
			}
		}

		// We do not look for an existing join on the same path, because
		// it makes sense to join twice on the same collection role
		FromElementFactory factory = new FromElementFactory(
				currentFromClause,
				lhsFromElement,
				propName,
				classAlias,
				getColumns(),
				implicitJoin
		);
		FromElement elem = factory.createCollection( queryableCollection, role, joinType, fetch, indexed );

		LOG.debugf( "dereferenceCollection() : Created new FROM element for %s : %s", propName, elem );

		setImpliedJoin( elem );
		setFromElement( elem );    // This 'dot' expression now refers to the resulting from element.

		if ( isSizeProperty ) {
			elem.setText( "" );
			elem.setUseWhereFragment( false );
		}

		if ( !implicitJoin ) {
			EntityPersister entityPersister = elem.getEntityPersister();
			if ( entityPersister != null ) {
				getWalker().addQuerySpaces( entityPersister.getQuerySpaces() );
			}
		}
		getWalker().addQuerySpaces( queryableCollection.getCollectionSpaces() );    // Always add the collection's query spaces.
	}

	private void dereferenceEntity(
			EntityType entityType,
			boolean implicitJoin,
			String classAlias,
			boolean generateJoin,
			AST parent,
			AST parentPredicate) throws SemanticException {
		checkForCorrelatedSubquery( "dereferenceEntity" );
		// three general cases we check here as to whether to render a physical SQL join:
		// 1) is our parent a DotNode as well?  If so, our property reference is
		// 		being further de-referenced...
		// 2) is this a DML statement
		// 3) we were asked to generate any needed joins (generateJoins==true) *OR*
		//		we are currently processing a select or from clause
		// (an additional check is the regressionStyleJoinSuppression check solely intended for the test suite)
		//
		// The regressionStyleJoinSuppression is an additional check
		// intended solely for use within the test suite.  This forces the
		// implicit join resolution to behave more like the classic parser.
		// The underlying issue is that classic translator is simply wrong
		// about its decisions on whether or not to render an implicit join
		// into a physical SQL join in a lot of cases.  The piece it generally
		// tends to miss is that INNER joins effect the results by further
		// restricting the data set!  A particular manifestation of this is
		// the fact that the classic translator will skip the physical join
		// for ToOne implicit joins *if the query is shallow*; the result
		// being that Query.list() and Query.iterate() could return
		// different number of results!
		DotNode parentAsDotNode = null;
		String property = propertyName;
		final boolean joinIsNeeded;

		if ( isDotNode( parent ) ) {
			// our parent is another dot node, meaning we are being further dereferenced.
			// thus we need to generate a join unless the association is non-nullable and
			// parent refers to the associated entity's PK (because 'our' table would know the FK).
			parentAsDotNode = (DotNode) parent;
			property = parentAsDotNode.propertyName;
			joinIsNeeded = generateJoin && (
					entityType.isNullable() ||
					!isPropertyEmbeddedInJoinProperties( parentAsDotNode.propertyName )
			);
		}
		else if ( !getWalker().isSelectStatement() ) {
			// in non-select queries, the only time we should need to join is if we are in a subquery from clause
			joinIsNeeded = getWalker().getCurrentStatementType() == SqlTokenTypes.SELECT && getWalker().isInFrom();
		}
		else if ( regressionStyleJoinSuppression ) {
			// this is the regression style determination which matches the logic of the classic translator
			joinIsNeeded = generateJoin && ( !getWalker().isInSelect() || !getWalker().isShallowQuery() );
		}
		else if ( parentPredicate != null ) {
			// Never generate a join when we compare entities directly
			joinIsNeeded = generateJoin;
		}
		else {
			joinIsNeeded = generateJoin || ( getWalker().isInSelect() || getWalker().isInFrom() );
		}

		if ( joinIsNeeded ) {
			dereferenceEntityJoin( classAlias, entityType, implicitJoin, parent );
		}
		else {
			dereferenceEntityIdentifier( property, parentAsDotNode );
		}

	}

	private static boolean isDotNode(AST n) {
		return n != null && n.getType() == SqlTokenTypes.DOT;
	}

	private void dereferenceEntityJoin(String classAlias, EntityType propertyType, boolean impliedJoin, AST parent)
			throws SemanticException {
		dereferenceType = DereferenceType.ENTITY;
		if ( LOG.isDebugEnabled() ) {
			LOG.debugf(
					"dereferenceEntityJoin() : generating join for %s in %s (%s) parent = %s",
					propertyName,
					getFromElement().getClassName(),
					classAlias == null ? "<no alias>" : classAlias,
					ASTUtil.getDebugString( parent )
			);
		}
		// Create a new FROM node for the referenced class.
		String associatedEntityName = propertyType.getAssociatedEntityName();
		String tableAlias = getAliasGenerator().createName( associatedEntityName );

		String[] joinColumns = getColumns();
		String joinPath = getPath();

		if ( impliedJoin && getWalker().isInFrom() ) {
			joinType = getWalker().getImpliedJoinType();
		}

		FromClause currentFromClause = getWalker().getCurrentFromClause();
		FromElement elem = currentFromClause.findJoinByPath( joinPath );

///////////////////////////////////////////////////////////////////////////////
//
// This is the piece which recognizes the condition where an implicit join path
// resolved earlier in a correlated subquery is now being referenced in the
// outer query.  For 3.0final, we just let this generate a second join (which
// is exactly how the old parser handles this).  Eventually we need to add this
// logic back in and complete the logic in FromClause.promoteJoin; however,
// FromClause.promoteJoin has its own difficulties (see the comments in
// FromClause.promoteJoin).
//
//		if ( elem == null ) {
//			// see if this joinPath has been used in a "child" FromClause, and if so
//			// promote that element to the outer query
//			FromClause currentNodeOwner = getFromElement().getFromClause();
//			FromClause currentJoinOwner = currentNodeOwner.locateChildFromClauseWithJoinByPath( joinPath );
//			if ( currentJoinOwner != null && currentNodeOwner != currentJoinOwner ) {
//				elem = currentJoinOwner.findJoinByPathLocal( joinPath );
//				if ( elem != null ) {
//					currentFromClause.promoteJoin( elem );
//					// EARLY EXIT!!!
//					return;
//				}
//			}
//		}
//
///////////////////////////////////////////////////////////////////////////////

		boolean found = elem != null;
		// even though we might find a pre-existing element by join path, we may not be able to reuse it...
		boolean useFoundFromElement = found && canReuse( classAlias, elem );

		if ( !useFoundFromElement ) {
			// If the lhs of the join is a "component join", we need to go back to the
			// first non-component-join as the origin to properly link aliases and
			// join columns
			FromElement lhsFromElement = getLhs().getFromElement();
			while ( lhsFromElement != null && ComponentJoin.class.isInstance( lhsFromElement ) ) {
				lhsFromElement = lhsFromElement.getOrigin();
			}
			if ( lhsFromElement == null ) {
				throw new QueryException( "Unable to locate appropriate lhs" );
			}

			String role = lhsFromElement.getClassName() + "." + propertyName;

			JoinSequence joinSequence;

			if ( joinColumns.length == 0 && lhsFromElement instanceof EntityQuerySpace ) {
				// When no columns are available, this is a special join that involves multiple subtypes
				String lhsTableAlias = getLhs().getFromElement().getTableAlias();

				AbstractEntityPersister persister = (AbstractEntityPersister) lhsFromElement.getEntityPersister();

				String[][] polyJoinColumns = persister.getPolymorphicJoinColumns(lhsTableAlias, propertyPath);

				// Special join sequence that uses the poly join columns
				joinSequence = getSessionFactoryHelper()
						.createJoinSequence( impliedJoin, propertyType, tableAlias, joinType, polyJoinColumns );
			}
			else {
				// If this is an implied join in a from element, then use the implied join type which is part of the
				// tree parser's state (set by the grammar actions).
				joinSequence = getSessionFactoryHelper()
						.createJoinSequence( impliedJoin, propertyType, tableAlias, joinType, joinColumns );
			}

			FromElementFactory factory = new FromElementFactory(
					currentFromClause,
					lhsFromElement,
					joinPath,
					classAlias,
					joinColumns,
					impliedJoin
			);
			elem = factory.createEntityJoin(
					associatedEntityName,
					tableAlias,
					joinSequence,
					fetch,
					getWalker().isInFrom(),
					propertyType,
					role,
					joinPath
			);
		}
		else {
			// NOTE : addDuplicateAlias() already performs nullness checks on the alias.
			currentFromClause.addDuplicateAlias( classAlias, elem );
		}
		setImpliedJoin( elem );
		getWalker().addQuerySpaces( elem.getEntityPersister().getQuerySpaces() );
		setFromElement( elem );    // This 'dot' expression now refers to the resulting from element.
	}

	private boolean canReuse(String classAlias, FromElement fromElement) {
		// if the from-clauses are the same, we can be a little more aggressive in terms of what we reuse
		if ( fromElement.getFromClause() == getWalker().getCurrentFromClause() &&
				areSame( classAlias, fromElement.getClassAlias() )) {
			return true;
		}

		// otherwise (subquery case) dont reuse the fromElement if we are processing the from-clause of the subquery
		return getWalker().getCurrentClauseType() != SqlTokenTypes.FROM;
	}

	private boolean areSame(String alias1, String alias2) {
		// again, null != null here
		return !StringHelper.isEmpty( alias1 ) && !StringHelper.isEmpty( alias2 ) && alias1.equals( alias2 );
	}

	private void setImpliedJoin(FromElement elem) {
		this.impliedJoin = elem;
		if ( getFirstChild().getType() == SqlTokenTypes.DOT ) {
			DotNode dotLhs = (DotNode) getFirstChild();
			if ( dotLhs.getImpliedJoin() != null ) {
				this.impliedJoin = dotLhs.getImpliedJoin();
			}
		}
	}

	@Override
	public FromElement getImpliedJoin() {
		return impliedJoin;
	}

	/**
	 * Is the given property name a reference to the join key of the associated
	 * entity constructed by the given entity type?
	 * <p/>
	 *
	 * This method resolves the {@code propertyName} as a property of the entity type at the
	 * {@link #propertyPath} relative to the {@link #getFromElement() FromElement}.
	 * The implementation does so by invoking {@link FromElement#getPropertyType(String, String)},
	 * which will resolve the property path against the entity's {@link org.hibernate.persister.entity.PropertyMapping}.
	 * On initialization of the {@link EntityPersister}, this {@code PropertyMapping} is filled with
	 * property paths for all the owned properties and associations, and (embedded) identifier or unique key
	 * properties for owned associations.
	 * Henceforth, whenever a property path is found in the {@code PropertyMapping} of the {@code EntityPersister}
	 * of the {@code FromElement}, we know that the property corresponds to a SQL fragment producible from the
	 * {@code FromElement}, and as such the entity property can be dereferenced (optimized) in the final query.
	 *
	 * <p/>
	 * For example, consider a fragment like order.customer.id
	 * (where order is a from-element alias).  Here, we'd have:
	 * propertyName = "id" AND
	 * propertyPath = "customer"
	 * FromElement = Order
	 * and are being asked to determine whether "customer.id" is a property path of Order
	 *
	 * @param propertyName The name of the property to check.
	 *
	 * @return True if propertyName references the entity's (owningType->associatedEntity)
	 *         join key; false otherwise.
	 */
	private boolean isPropertyEmbeddedInJoinProperties(String propertyName) {
		String propertyPath = String.join( ".", this.propertyPath, propertyName );
		try {
			Type propertyType = getFromElement().getPropertyType( this.propertyPath, propertyPath );
			return propertyType != null;
		}
		catch (QueryException e) {
			return false;
		}
	}

	private void checkForCorrelatedSubquery(String methodName) {
		if ( isCorrelatedSubselect() ) {
			LOG.debugf( "%s() : correlated subquery", methodName );
		}
	}

	private boolean isCorrelatedSubselect() {
		return getWalker().isSubQuery() &&
				getFromElement().getFromClause() != getWalker().getCurrentFromClause();
	}

	private void checkLhsIsNotCollection() throws SemanticException {
		if ( getLhs().getDataType() != null && getLhs().getDataType().isCollectionType() ) {
			throw ILLEGAL_COLL_DEREF_EXCP_BUILDER.buildIllegalCollectionDereferenceException( propertyName, getLhs() );
		}
	}

	private void dereferenceComponent(AST parent) {
		dereferenceType = DereferenceType.COMPONENT;
		setPropertyNameAndPath( parent );
	}

	private void dereferenceEntityIdentifier(String propertyName, DotNode dotParent) {
		// special shortcut for id properties, skip the join!
		// this must only occur at the _end_ of a path expression
		if ( LOG.isDebugEnabled() ) {
			LOG.debugf(
					"dereferenceShortcut() : property %s in %s does not require a join.",
					propertyName,
					getFromElement().getClassName()
			);
		}

		setPropertyNameAndPath( dotParent ); // Set the unresolved path in this node and the parent.
		initText();

		// Set the text for the parent.
		if ( dotParent != null ) {
			dotParent.dereferenceType = DereferenceType.IDENTIFIER;
			dotParent.setText( getText() );
			dotParent.columns = getColumns();
		}
	}

	private void setPropertyNameAndPath(AST parent) {
		if ( isDotNode( parent ) ) {
			DotNode dotNode = (DotNode) parent;
			AST lhs = dotNode.getFirstChild();
			AST rhs = lhs.getNextSibling();
			propertyName = rhs.getText();
			propertyPath = propertyPath + "." + propertyName; // Append the new property name onto the unresolved path.
			dotNode.propertyPath = propertyPath;
			LOG.debugf( "Unresolved property path is now '%s'", dotNode.propertyPath );
		}
		else {
			LOG.debugf( "Terminal getPropertyPath = [%s]", propertyPath );
		}
	}

	@Override
	public Type getDataType() {
		if ( super.getDataType() == null ) {
			FromElement fromElement = getLhs().getFromElement();
			if ( fromElement == null ) {
				return null;
			}
			// If the lhs is a collection, use CollectionPropertyMapping
			Type propertyType = fromElement.getPropertyType( propertyPath, propertyPath );
			LOG.debugf( "getDataType() : %s -> %s", propertyPath, propertyType );
			super.setDataType( propertyType );
		}
		return super.getDataType();
	}

	@Override
	public String[] getReferencedTables() {
		FromReferenceNode lhs = ( (FromReferenceNode) getFirstChild() );
		if ( lhs != null) {
			FromElement fromElement = lhs.getFromElement();
			if ( fromElement != null ) {
				String propertyTableName = fromElement.getPropertyTableName( propertyPath );
				return new String[] { propertyTableName };
			}
		}
		return null;
	}

	public void setPropertyPath(String propertyPath) {
		this.propertyPath = propertyPath;
	}

	public String getPropertyPath() {
		return propertyPath;
	}

	public FromReferenceNode getLhs() {
		FromReferenceNode lhs = ( (FromReferenceNode) getFirstChild() );
		if ( lhs == null ) {
			throw new IllegalStateException( "DOT node with no left-hand-side!" );
		}
		return lhs;
	}

	/**
	 * Returns the full path of the node.
	 *
	 * @return the full path of the node.
	 */
	@Override
	public String getPath() {
		if ( path == null ) {
			FromReferenceNode lhs = getLhs();
			if ( lhs == null ) {
				path = getText();
			}
			else {
				SqlNode rhs = (SqlNode) lhs.getNextSibling();
				path = lhs.getPath() + "." + rhs.getOriginalText();
			}
		}
		return path;
	}

	public void setFetch(boolean fetch) {
		this.fetch = fetch;
	}

	public void setScalarColumnText(int i) throws SemanticException {
		String[] sqlColumns = getColumns();
		ColumnHelper.generateScalarColumns( this, sqlColumns, i );
	}

	/**
	 * Special method to resolve expressions in the SELECT list.
	 *
	 * @throws SemanticException if this cannot be resolved.
	 */
	public void resolveSelectExpression() throws SemanticException {
		if ( getWalker().isShallowQuery() || getWalker().getCurrentFromClause().isSubQuery() ) {
			resolve( false, true );
		}
		else {
			resolve( true, false );
			Type type = getDataType();
			if ( type.isEntityType() ) {
				FromElement fromElement = getFromElement();
				fromElement.setIncludeSubclasses( true ); // Tell the destination fromElement to 'includeSubclasses'.
				if ( useThetaStyleImplicitJoins ) {
					fromElement.getJoinSequence().setUseThetaStyle( true );    // Use theta style (for regression)
					// Move the node up, after the origin node.
					FromElement origin = fromElement.getOrigin();
					if ( origin != null ) {
						ASTUtil.makeSiblingOfParent( origin, fromElement );
					}
				}
			}
		}

		FromReferenceNode lhs = getLhs();
		while ( lhs != null ) {
			checkSubclassOrSuperclassPropertyReference( lhs, lhs.getNextSibling().getText() );
			lhs = (FromReferenceNode) lhs.getFirstChild();
		}
	}

	public void setResolvedConstant(String text) {
		path = text;
		dereferenceType = DereferenceType.JAVA_CONSTANT;
		setResolved(); // Don't resolve the node again.
	}

	private boolean checkSubclassOrSuperclassPropertyReference(FromReferenceNode lhs, String propertyName) {
		if ( lhs != null && !( lhs instanceof IndexNode ) ) {
			final FromElement source = lhs.getFromElement();
			if ( source != null ) {
				source.handlePropertyBeingDereferenced( lhs.getDataType(), propertyName );
			}
		}
		return false;
	}
}

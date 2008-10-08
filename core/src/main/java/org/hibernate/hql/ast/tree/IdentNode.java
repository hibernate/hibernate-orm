/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.hql.ast.tree;

import antlr.SemanticException;
import antlr.collections.AST;
import org.hibernate.QueryException;
import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.hql.antlr.SqlTokenTypes;
import org.hibernate.hql.ast.util.ColumnHelper;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.sql.JoinFragment;
import org.hibernate.type.CollectionType;
import org.hibernate.type.Type;
import org.hibernate.util.StringHelper;

import java.util.List;

/**
 * Represents an identifier all by itself, which may be a function name,
 * a class alias, or a form of naked property-ref depending on the
 * context.
 *
 * @author josh
 */
public class IdentNode extends FromReferenceNode implements SelectExpression {

	private static final int UNKNOWN = 0;
	private static final int PROPERTY_REF = 1;
	private static final int COMPONENT_REF = 2;

	private boolean nakedPropertyRef = false;

	public void resolveIndex(AST parent) throws SemanticException {
		// An ident node can represent an index expression if the ident
		// represents a naked property ref
		//      *Note: this makes the assumption (which is currently the case
		//      in the hql-sql grammar) that the ident is first resolved
		//      itself (addrExpr -> resolve()).  The other option, if that
		//      changes, is to call resolve from here; but it is
		//      currently un-needed overhead.
		if (!(isResolved() && nakedPropertyRef)) {
			throw new UnsupportedOperationException();
		}

		String propertyName = getOriginalText();
		if (!getDataType().isCollectionType()) {
			throw new SemanticException("Collection expected; [" + propertyName + "] does not refer to a collection property");
		}

		// TODO : most of below was taken verbatim from DotNode; should either delegate this logic or super-type it
		CollectionType type = (CollectionType) getDataType();
		String role = type.getRole();
		QueryableCollection queryableCollection = getSessionFactoryHelper().requireQueryableCollection(role);

		String alias = null;  // DotNode uses null here...
		String columnTableAlias = getFromElement().getTableAlias();
		int joinType = JoinFragment.INNER_JOIN;
		boolean fetch = false;

		FromElementFactory factory = new FromElementFactory(
				getWalker().getCurrentFromClause(),
				getFromElement(),
				propertyName,
				alias,
				getFromElement().toColumns(columnTableAlias, propertyName, false),
				true
		);
		FromElement elem = factory.createCollection(queryableCollection, role, joinType, fetch, true);
		setFromElement(elem);
		getWalker().addQuerySpaces(queryableCollection.getCollectionSpaces());	// Always add the collection's query spaces.
	}

	public void resolve(boolean generateJoin, boolean implicitJoin, String classAlias, AST parent) {
		if (!isResolved()) {
			if (getWalker().getCurrentFromClause().isFromElementAlias(getText())) {
				if (resolveAsAlias()) {
					setResolved();
					// We represent a from-clause alias
				}
			}
			else if (parent != null && parent.getType() == SqlTokenTypes.DOT) {
				DotNode dot = (DotNode) parent;
				if (parent.getFirstChild() == this) {
					if (resolveAsNakedComponentPropertyRefLHS(dot)) {
						// we are the LHS of the DOT representing a naked comp-prop-ref
						setResolved();
					}
				}
				else {
					if (resolveAsNakedComponentPropertyRefRHS(dot)) {
						// we are the RHS of the DOT representing a naked comp-prop-ref
						setResolved();
					}
				}
			}
			else {
				int result = resolveAsNakedPropertyRef();
				if (result == PROPERTY_REF) {
					// we represent a naked (simple) prop-ref
					setResolved();
				}
				else if (result == COMPONENT_REF) {
					// EARLY EXIT!!!  return so the resolve call explicitly coming from DotNode can
					// resolve this...
					return;
				}
			}

			// if we are still not resolved, we might represent a constant.
			//      needed to add this here because the allowance of
			//      naked-prop-refs in the grammar collides with the
			//      definition of literals/constants ("nondeterminism").
			//      TODO: cleanup the grammar so that "processConstants" is always just handled from here
			if (!isResolved()) {
				try {
					getWalker().getLiteralProcessor().processConstant(this, false);
				}
				catch (Throwable ignore) {
					// just ignore it for now, it'll get resolved later...
				}
			}
		}
	}

	private boolean resolveAsAlias() {
		// This is not actually a constant, but a reference to FROM element.
		FromElement element = getWalker().getCurrentFromClause().getFromElement(getText());
		if (element != null) {
			setFromElement(element);
			setText(element.getIdentityColumn());
			setType(SqlTokenTypes.ALIAS_REF);
			return true;
		}
		return false;
	}

	private Type getNakedPropertyType(FromElement fromElement)
	{
		if (fromElement == null) {
			return null;
		}
		String property = getOriginalText();
		Type propertyType = null;
		try {
			propertyType = fromElement.getPropertyType(property, property);
		}
		catch (Throwable t) {
		}
		return propertyType;
	}

	private int resolveAsNakedPropertyRef() {
		FromElement fromElement = locateSingleFromElement();
		if (fromElement == null) {
			return UNKNOWN;
		}
		Queryable persister = fromElement.getQueryable();
		if (persister == null) {
			return UNKNOWN;
		}
		Type propertyType = getNakedPropertyType(fromElement);
		if (propertyType == null) {
			// assume this ident's text does *not* refer to a property on the given persister
			return UNKNOWN;
		}

		if ((propertyType.isComponentType() || propertyType.isAssociationType() )) {
			return COMPONENT_REF;
		}

		setFromElement(fromElement);
		String property = getText();
		String[] columns = getWalker().isSelectStatement()
				? persister.toColumns(fromElement.getTableAlias(), property)
				: persister.toColumns(property);
		String text = StringHelper.join(", ", columns);
		setText(columns.length == 1 ? text : "(" + text + ")");
		setType(SqlTokenTypes.SQL_TOKEN);

		// these pieces are needed for usage in select clause
		super.setDataType(propertyType);
		nakedPropertyRef = true;

		return PROPERTY_REF;
	}

	private boolean resolveAsNakedComponentPropertyRefLHS(DotNode parent) {
		FromElement fromElement = locateSingleFromElement();
		if (fromElement == null) {
			return false;
		}

		Type componentType = getNakedPropertyType(fromElement);
		if ( componentType == null ) {
			throw new QueryException( "Unable to resolve path [" + parent.getPath() + "], unexpected token [" + getOriginalText() + "]" );
		}
		if (!componentType.isComponentType()) {
			throw new QueryException("Property '" + getOriginalText() + "' is not a component.  Use an alias to reference associations or collections.");
		}

		Type propertyType = null;  // used to set the type of the parent dot node
		String propertyPath = getText() + "." + getNextSibling().getText();
		try {
			// check to see if our "propPath" actually
			// represents a property on the persister
			propertyType = fromElement.getPropertyType(getText(), propertyPath);
		}
		catch (Throwable t) {
			// assume we do *not* refer to a property on the given persister
			return false;
		}

		setFromElement(fromElement);
		parent.setPropertyPath(propertyPath);
		parent.setDataType(propertyType);

		return true;
	}

	private boolean resolveAsNakedComponentPropertyRefRHS(DotNode parent) {
		FromElement fromElement = locateSingleFromElement();
		if (fromElement == null) {
			return false;
		}

		Type propertyType = null;
		String propertyPath = parent.getLhs().getText() + "." + getText();
		try {
			// check to see if our "propPath" actually
			// represents a property on the persister
			propertyType = fromElement.getPropertyType(getText(), propertyPath);
		}
		catch (Throwable t) {
			// assume we do *not* refer to a property on the given persister
			return false;
		}

		setFromElement(fromElement);
		// this piece is needed for usage in select clause
		super.setDataType(propertyType);
		nakedPropertyRef = true;

		return true;
	}

	private FromElement locateSingleFromElement() {
		List fromElements = getWalker().getCurrentFromClause().getFromElements();
		if (fromElements == null || fromElements.size() != 1) {
			// TODO : should this be an error?
			return null;
		}
		FromElement element = (FromElement) fromElements.get(0);
		if (element.getClassAlias() != null) {
			// naked property-refs cannot be used with an aliased from element
			return null;
		}
		return element;
	}

	public Type getDataType() {
		Type type = super.getDataType();
		if ( type != null ) {
			return type;
		}
		FromElement fe = getFromElement();
		if ( fe != null ) {
			return fe.getDataType();
		}
		SQLFunction sf = getWalker().getSessionFactoryHelper().findSQLFunction( getText() );
		return sf == null ? null : sf.getReturnType( null, null );
	}

	public void setScalarColumnText(int i) throws SemanticException {
		if (nakedPropertyRef) {
			// do *not* over-write the column text, as that has already been
			// "rendered" during resolve
			ColumnHelper.generateSingleScalarColumn(this, i);
		}
		else {
			FromElement fe = getFromElement();
			if (fe != null) {
				setText(fe.renderScalarIdentifierSelect(i));
			}
			else {
				ColumnHelper.generateSingleScalarColumn(this, i);
			}
		}
	}

	public String getDisplayText() {
		StringBuffer buf = new StringBuffer();

		if (getType() == SqlTokenTypes.ALIAS_REF) {
			buf.append("{alias=").append(getOriginalText());
			if (getFromElement() == null) {
				buf.append(", no from element");
			}
			else {
				buf.append(", className=").append(getFromElement().getClassName());
				buf.append(", tableAlias=").append(getFromElement().getTableAlias());
			}
			buf.append("}");
		}
		else {
			buf.append("{originalText=" + getOriginalText()).append("}");
		}
		return buf.toString();
	}

}

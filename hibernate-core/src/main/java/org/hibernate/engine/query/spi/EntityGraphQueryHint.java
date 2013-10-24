/* 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.engine.query.spi;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.persistence.AttributeNode;
import javax.persistence.EntityGraph;
import javax.persistence.Subgraph;

import org.hibernate.annotations.common.util.StringHelper;
import org.hibernate.engine.internal.JoinSequence;
import org.hibernate.hql.internal.ast.HqlSqlWalker;
import org.hibernate.hql.internal.ast.tree.FromClause;
import org.hibernate.hql.internal.ast.tree.FromElement;
import org.hibernate.hql.internal.ast.tree.FromElementFactory;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.sql.JoinType;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;

/**
 * Encapsulates a JPA EntityGraph provided through a JPQL query hint.  Converts the fetches into a list of AST
 * FromElements.  The logic is kept here as much as possible in order to make it easy to remove this in the future,
 * once our AST is improved and this "hack" is no longer needed.
 * 
 * @author Brett Meyer
 */
public class EntityGraphQueryHint {
	
	private final EntityGraph<?> originEntityGraph;
	
	public EntityGraphQueryHint( EntityGraph<?> originEntityGraph ) {
		this.originEntityGraph = originEntityGraph;
	}
	
	public List<FromElement> toFromElements(FromClause fromClause, HqlSqlWalker walker) {
		List<String> roles = new ArrayList<String>();
		Iterator iter = fromClause.getFromElements().iterator();
		while ( iter.hasNext() ) {
			final FromElement fromElement = ( FromElement ) iter.next();
			if (fromElement.getRole() != null) {
				roles.add( fromElement.getRole() );
			}
		}
		
		return getFromElements( originEntityGraph.getAttributeNodes(), fromClause.getFromElement(), fromClause, walker );
	}
	
	private List<FromElement> getFromElements(List attributeNodes, FromElement origin, FromClause fromClause, HqlSqlWalker walker) {
		final List<FromElement> fromElements = new ArrayList<FromElement>();
		
		for (Object obj : attributeNodes) {			
			final AttributeNode<?> attributeNode = (AttributeNode<?>) obj;
			
			final String className = origin.getClassName();
			final String attributeName = attributeNode.getAttributeName();
			final String role = className + "." + attributeName;
			
			final String classAlias = origin.getClassAlias();
			String path = attributeName;
			if (!StringHelper.isEmpty( classAlias )) {
				path = classAlias + "." + path;
			}
			
			final String originTableAlias = origin.getTableAlias();
			
			Type propertyType = origin.getPropertyType( attributeName, path );
			
			try {
				FromElement fromElement = null;
				if ( propertyType.isEntityType() ) {
					final EntityType entityType = (EntityType) propertyType;
					
					final String[] columns = origin.toColumns( originTableAlias, path, false );
					final String tableAlias = walker.getAliasGenerator().createName(
							entityType.getAssociatedEntityName() );	
					
					final FromElementFactory fromElementFactory = new FromElementFactory( fromClause, origin, path, classAlias,
							columns, false);
					// TODO: impliedJoin?
					final JoinSequence joinSequence = walker.getSessionFactoryHelper().createJoinSequence(
							false, entityType, tableAlias, JoinType.LEFT_OUTER_JOIN, columns );
					fromElement = fromElementFactory.createEntityJoin(
							entityType.getAssociatedEntityName(), tableAlias, joinSequence, true, walker.isInFrom(), entityType );
				}
				else if ( propertyType.isCollectionType() ) {
					final String[] columns = origin.toColumns( originTableAlias, path, false );		
					
					final FromElementFactory fromElementFactory = new FromElementFactory( fromClause, origin, path, classAlias,
							columns, false);
					final QueryableCollection queryableCollection = walker.getSessionFactoryHelper()
							.requireQueryableCollection( role );
					fromElement = fromElementFactory.createCollection(
							queryableCollection, role, JoinType.LEFT_OUTER_JOIN, true, false ) ;
				}
				
				if (fromElement != null) {
					fromElements.add( fromElement );
					
					// recurse into subgraphs
					for (Subgraph<?> subgraph : attributeNode.getSubgraphs().values()) {
						fromElements.addAll( getFromElements( subgraph.getAttributeNodes(), fromElement,
								fromClause, walker ) );
					}
				}
			}
			catch (Exception e) {
				// TODO
			}
		}
		
		return fromElements;
	}
}

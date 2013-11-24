/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009 by Red Hat Inc and/or its affiliates or by
 * third-party contributors as indicated by either @author tags or express
 * copyright attribution statements applied by the authors.  All
 * third-party contributions are distributed under license by Red Hat Inc.
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
package org.hibernate.hql.internal.ast.tree;

import org.hibernate.QueryException;
import org.hibernate.hql.internal.NameGenerator;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.persister.entity.PropertyMapping;
import org.hibernate.type.ComponentType;
import org.hibernate.type.Type;

/**
 * Models an explicit join terminating at a component value (e.g. <tt>... from Person p join p.name as n ...</tt>)
 *
 * @author Steve Ebersole
 */
public class ComponentJoin extends FromElement {
	private final String componentPath;
	private final ComponentType componentType;

	private final String componentProperty;
	private final String[] columns;
	private final String columnsFragment;

	public ComponentJoin(
			FromClause fromClause,
			FromElement origin,
			String alias,
			String componentPath,
			ComponentType componentType) {
		super( fromClause, origin, alias );
		this.componentPath = componentPath;
		this.componentType = componentType;
		this.componentProperty = StringHelper.unqualify( componentPath );
		fromClause.addJoinByPathMap( componentPath, this );
		initializeComponentJoin( new ComponentFromElementType( this ) );

		this.columns = origin.getPropertyMapping( "" ).toColumns( getTableAlias(), componentProperty );
		StringBuilder buf = new StringBuilder();
		for ( int j = 0; j < columns.length; j++ ) {
			final String column = columns[j];
			if ( j > 0 ) {
				buf.append( ", " );
			}
			buf.append( column );
		}
		this.columnsFragment = buf.toString();
	}

	public String getComponentPath() {
		return componentPath;
	}

	public String getComponentProperty() {
		return componentProperty;
	}

	public ComponentType getComponentType() {
		return componentType;
	}

	@Override
	public Type getDataType() {
		return getComponentType();
	}

	@Override
	public String getIdentityColumn() {
		// used to "resolve" the IdentNode when our alias is encountered *by itself* in the query; so
		//		here we use the component
		// NOTE : ^^ is true *except for* when encountered by itself in the SELECT clause.  That gets
		// 		routed through org.hibernate.hql.internal.ast.tree.ComponentJoin.ComponentFromElementType.renderScalarIdentifierSelect()
		//		which we also override to account for
		return columnsFragment;
	}

	@Override
	public String[] getIdentityColumns() {
		return columns;
	}

	@Override
	public String getDisplayText() {
		return "ComponentJoin{path=" + getComponentPath() + ", type=" + componentType.getReturnedClass() + "}";
	}

	public class ComponentFromElementType extends FromElementType {
		private final PropertyMapping propertyMapping = new ComponentPropertyMapping();

		public ComponentFromElementType(FromElement fromElement) {
			super( fromElement );
		}

		@Override
		public Type getDataType() {
			return getComponentType();
		}

		@Override
		public QueryableCollection getQueryableCollection() {
			return null;
		}

		@Override
		public PropertyMapping getPropertyMapping(String propertyName) {
			return propertyMapping;
		}

		@Override
		public Type getPropertyType(String propertyName, String propertyPath) {
			int index = getComponentType().getPropertyIndex( propertyName );
			return getComponentType().getSubtypes()[index];
		}

		@Override
		public String renderScalarIdentifierSelect(int i) {
			String[] cols = getBasePropertyMapping().toColumns( getTableAlias(), getComponentProperty() );
			StringBuilder buf = new StringBuilder();
			// For property references generate <tablealias>.<columnname> as <projectionalias>
			for ( int j = 0; j < cols.length; j++ ) {
				final String column = cols[j];
				if ( j > 0 ) {
					buf.append( ", " );
				}
				buf.append( column ).append( " as " ).append( NameGenerator.scalarName( i, j ) );
			}
			return buf.toString();
		}
	}

	protected PropertyMapping getBasePropertyMapping() {
		return getOrigin().getPropertyMapping( "" );
	}

	private final class ComponentPropertyMapping implements PropertyMapping {
		@Override
		public Type getType() {
			return getComponentType();
		}

		@Override
		public Type toType(String propertyName) throws QueryException {
			return getBasePropertyMapping().toType( getPropertyPath( propertyName ) );
		}

		protected String getPropertyPath(String propertyName) {
			return getComponentPath() + '.' + propertyName;
		}

		@Override
		public String[] toColumns(String alias, String propertyName) throws QueryException {
			return getBasePropertyMapping().toColumns( alias, getPropertyPath( propertyName ) );
		}

		@Override
		public String[] toColumns(String propertyName) throws QueryException, UnsupportedOperationException {
			return getBasePropertyMapping().toColumns( getPropertyPath( propertyName ) );
		}
	}
}

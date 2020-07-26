/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.ast.tree;

import org.hibernate.QueryException;
import org.hibernate.hql.internal.NameGenerator;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.persister.collection.QueryableCollection;
import org.hibernate.persister.entity.PropertyMapping;
import org.hibernate.type.CompositeType;
import org.hibernate.type.Type;

/**
 * Models an explicit join terminating at a component value (e.g. <tt>... from Person p join p.name as n ...</tt>)
 *
 * @author Steve Ebersole
 */
public class ComponentJoin extends FromElement {
	private final String componentPath;
	private final CompositeType componentType;

	private final String componentProperty;
	private final String[] columns;
	private final String columnsFragment;

	public ComponentJoin(
			FromClause fromClause,
			FromElement origin,
			String alias,
			String componentPath,
			CompositeType componentType) {
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

	public CompositeType getComponentType() {
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

		// `size( c.component.customers )`
		// PropertyMapping(c).toColumns( component.customers )

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

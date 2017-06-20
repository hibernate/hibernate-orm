/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.criterion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.type.Type;

/**
 * A projection that wraps other projections to allow selecting multiple values.
 *
 * @author Gavin King
 */
public class ProjectionList implements EnhancedProjection {
	private List<Projection> elements = new ArrayList<Projection>();

	/**
	 * Constructs a ProjectionList
	 *
	 * @see Projections#projectionList()
	 */
	protected ProjectionList() {
	}

	/**
	 * Lol
	 *
	 * @return duh
	 *
	 * @deprecated an instance factory method does not make sense
	 *
	 * @see Projections#projectionList()
	 */
	@Deprecated
	public ProjectionList create() {
		return new ProjectionList();
	}

	/**
	 * Add a projection to this list of projections
	 *
	 * @param projection The projection to add
	 *
	 * @return {@code this}, for method chaining
	 */
	public ProjectionList add(Projection projection) {
		elements.add( projection );
		return this;
	}

	/**
	 * Adds a projection to this list of projections after wrapping it with an alias
	 *
	 * @param projection The projection to add
	 * @param alias The alias to apply to the projection
	 *
	 * @return {@code this}, for method chaining
	 *
	 * @see Projections#alias
	 */
	public ProjectionList add(Projection projection, String alias) {
		return add( Projections.alias( projection, alias ) );
	}

	@Override
	public boolean isGrouped() {
		for ( Projection projection : elements ) {
			if ( projection.isGrouped() ) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Type[] getTypes(Criteria criteria, CriteriaQuery criteriaQuery) throws HibernateException {
		final List<Type> types = new ArrayList<Type>( getLength() );
		for ( Projection projection : elements ) {
			final Type[] elemTypes = projection.getTypes( criteria, criteriaQuery );
			Collections.addAll( types, elemTypes );
		}
		return types.toArray( new Type[types.size()] );
	}

	@Override
	public String toSqlString(Criteria criteria, int loc, CriteriaQuery criteriaQuery) throws HibernateException {
		final StringBuilder buf = new StringBuilder();
		String separator = "";

		for ( Projection projection : elements ) {
			buf.append( separator ).append( projection.toSqlString( criteria, loc, criteriaQuery ) );
			loc += getColumnAliases( loc, criteria, criteriaQuery, projection ).length;
			separator = ", ";
		}
		return buf.toString();
	}

	@Override
	public String toGroupSqlString(Criteria criteria, CriteriaQuery criteriaQuery) throws HibernateException {
		final StringBuilder buf = new StringBuilder();
		String separator = "";
		for ( Projection projection : elements ) {
			if ( ! projection.isGrouped() ) {
				continue;
			}

			buf.append( separator ).append( projection.toGroupSqlString( criteria, criteriaQuery ) );
			separator = ", ";
		}
		return buf.toString();
	}

	@Override
	public String[] getColumnAliases(final int loc) {
		int position = loc;
		final List<String> result = new ArrayList<String>( getLength() );
		for ( Projection projection : elements ) {
			final String[] aliases = projection.getColumnAliases( position );
			Collections.addAll( result, aliases );
			position += aliases.length;
		}
		return result.toArray( new String[ result.size() ] );
	}

	@Override
	public String[] getColumnAliases(final int loc, Criteria criteria, CriteriaQuery criteriaQuery) {
		int position = loc;
		final List<String> result = new ArrayList<String>( getLength() );
		for ( Projection projection : elements ) {
			final String[] aliases = getColumnAliases( position, criteria, criteriaQuery, projection );
			Collections.addAll( result, aliases );
			position += aliases.length;
		}
		return result.toArray( new String[result.size()] );
	}

	@Override
	public String[] getColumnAliases(String alias, final int loc) {
		int position = loc;
		for ( Projection projection : elements ) {
			final String[] aliases = projection.getColumnAliases( alias, position );
			if ( aliases != null ) {
				return aliases;
			}
			position += projection.getColumnAliases( position ).length;
		}
		return null;
	}

	@Override
	public String[] getColumnAliases(String alias, int loc, Criteria criteria, CriteriaQuery criteriaQuery) {
		int position = loc;
		for ( Projection projection : elements ) {
			final String[] aliases = getColumnAliases( alias, position, criteria, criteriaQuery, projection );
			if ( aliases != null ) {
				return aliases;
			}
			position += getColumnAliases( position, criteria, criteriaQuery, projection ).length;
		}
		return null;
	}

	private static String[] getColumnAliases(int loc, Criteria criteria, CriteriaQuery criteriaQuery, Projection projection) {
		return projection instanceof EnhancedProjection
				? ( (EnhancedProjection) projection ).getColumnAliases( loc, criteria, criteriaQuery )
				: projection.getColumnAliases( loc );
	}

	private static String[] getColumnAliases(String alias, int loc, Criteria criteria, CriteriaQuery criteriaQuery, Projection projection) {
		return projection instanceof EnhancedProjection
				? ( (EnhancedProjection) projection ).getColumnAliases( alias, loc, criteria, criteriaQuery )
				: projection.getColumnAliases( alias, loc );
	}

	@Override
	public Type[] getTypes(String alias, Criteria criteria, CriteriaQuery criteriaQuery) {
		for ( Projection projection : elements ) {
			final Type[] types = projection.getTypes( alias, criteria, criteriaQuery );
			if ( types != null ) {
				return types;
			}
		}
		return null;
	}

	@Override
	public String[] getAliases() {
		final List<String> result = new ArrayList<String>( getLength() );
		for ( Projection projection : elements ) {
			final String[] aliases = projection.getAliases();
			Collections.addAll( result, aliases );
		}
		return result.toArray( new String[result.size()] );
	}

	/**
	 * Access a wrapped projection by index
	 *
	 * @param i The index of the projection to return
	 *
	 * @return The projection
	 */
	@SuppressWarnings("UnusedDeclaration")
	public Projection getProjection(int i) {
		return elements.get( i );
	}

	public int getLength() {
		return elements.size();
	}

	@Override
	public String toString() {
		return elements.toString();
	}

}

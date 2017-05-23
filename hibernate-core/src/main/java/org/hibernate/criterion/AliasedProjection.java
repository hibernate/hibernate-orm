/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.criterion;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.type.Type;

/**
 * Represents a projection that specifies an alias
 *
 * @author Gavin King
 */
public class AliasedProjection implements EnhancedProjection {
	private final Projection projection;
	private final String alias;

	protected AliasedProjection(Projection projection, String alias) {
		this.projection = projection;
		this.alias = alias;
	}

	@Override
	public String toSqlString(Criteria criteria, int position, CriteriaQuery criteriaQuery) throws HibernateException {
		return projection.toSqlString( criteria, position, criteriaQuery );
	}

	@Override
	public String toGroupSqlString(Criteria criteria, CriteriaQuery criteriaQuery) {
		return projection.toGroupSqlString( criteria, criteriaQuery );
	}

	@Override
	public Type[] getTypes(Criteria criteria, CriteriaQuery criteriaQuery) throws HibernateException {
		return projection.getTypes( criteria, criteriaQuery );
	}

	@Override
	public String[] getColumnAliases(int loc) {
		return projection.getColumnAliases( loc );
	}

	@Override
	public String[] getColumnAliases(int loc, Criteria criteria, CriteriaQuery criteriaQuery) {
		return projection instanceof EnhancedProjection
				? ( (EnhancedProjection) projection ).getColumnAliases( loc, criteria, criteriaQuery )
				: getColumnAliases( loc );
	}

	@Override
	public Type[] getTypes(String alias, Criteria criteria, CriteriaQuery criteriaQuery) throws HibernateException {
		return this.alias.equals( alias )
				? getTypes( criteria, criteriaQuery )
				: null;
	}

	@Override
	public String[] getColumnAliases(String alias, int loc) {
		return this.alias.equals( alias )
				? getColumnAliases( loc )
				: null;
	}

	@Override
	public String[] getColumnAliases(String alias, int loc, Criteria criteria, CriteriaQuery criteriaQuery) {
		return this.alias.equals( alias )
				? getColumnAliases( loc, criteria, criteriaQuery )
				: null;
	}

	@Override
	public String[] getAliases() {
		return new String[] { alias };
	}

	@Override
	public boolean isGrouped() {
		return projection.isGrouped();
	}

	@Override
	public String toString() {
		return projection.toString() + " as " + alias;
	}

}

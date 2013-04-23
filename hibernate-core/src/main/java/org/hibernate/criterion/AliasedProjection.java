/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, 2013, Red Hat Inc. or third-party contributors as
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

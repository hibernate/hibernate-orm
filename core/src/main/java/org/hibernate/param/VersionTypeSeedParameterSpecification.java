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
package org.hibernate.param;

import org.hibernate.engine.QueryParameters;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.type.VersionType;
import org.hibernate.type.Type;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Parameter bind specification used for optimisitc lock version seeding (from insert statements).
 *
 * @author Steve Ebersole
 */
public class VersionTypeSeedParameterSpecification implements ParameterSpecification {
	private VersionType type;

	/**
	 * Constructs a version seed parameter bind specification.
	 *
	 * @param type The version type.
	 */
	public VersionTypeSeedParameterSpecification(VersionType type) {
		this.type = type;
	}

	/**
	 * {@inheritDoc}
	 */
	public int bind(PreparedStatement statement, QueryParameters qp, SessionImplementor session, int position)
	        throws SQLException {
		type.nullSafeSet( statement, type.seed( session ), position, session );
		return 1;
	}

	/**
	 * {@inheritDoc}
	 */
	public Type getExpectedType() {
		return type;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setExpectedType(Type expectedType) {
		// expected type is intrinsic here...
	}

	/**
	 * {@inheritDoc}
	 */
	public String renderDisplayInfo() {
		return "version-seed, type=" + type;
	}
}

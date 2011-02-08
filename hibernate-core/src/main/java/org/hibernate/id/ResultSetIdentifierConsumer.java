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
package org.hibernate.id;
import java.io.Serializable;
import java.sql.ResultSet;

/**
 * An optional contract for {@link org.hibernate.type.Type} or
 * {@link org.hibernate.usertype.UserType} implementations to handle generated
 * id values any way they see fit as opposed to being limited to the discrete set of
 * numeric types handled by {@link IdentifierGeneratorHelper}
 *
 * @author Steve Ebersole
 */
public interface ResultSetIdentifierConsumer {
	/**
	 * Given a result set, consume/extract the necessary values and construct an
	 * appropriate identifier value.
	 *
	 * @param resultSet The result set containing the value(s) to be used in building
	 * the identifier value.
	 * @return The identifier value.
	 */
	public Serializable consumeIdentifier(ResultSet resultSet);
}

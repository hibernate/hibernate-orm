/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.loader.plan.spi;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.internal.ResultSetProcessingContextImpl;
import org.hibernate.loader.spi.ResultSetProcessingContext;
import org.hibernate.type.Type;

/**
 * Represent a simple scalar return within a query result.  Generally this would be values of basic (String, Integer,
 * etc) or composite types.
 *
 * @author Steve Ebersole
 */
public class ScalarReturn extends AbstractPlanNode implements Return {
	private final Type type;
	private final String[] columnAliases;

	public ScalarReturn(SessionFactoryImplementor factory, Type type, String[] columnAliases) {
		super( factory );
		this.type = type;
		this.columnAliases = columnAliases;
	}

	public Type getType() {
		return type;
	}

	@Override
	public void hydrate(ResultSet resultSet, ResultSetProcessingContext context) {
		// nothing to do
	}

	@Override
	public void resolve(ResultSet resultSet, ResultSetProcessingContext context) {
		// nothing to do
	}

	@Override
	public Object read(ResultSet resultSet, ResultSetProcessingContext context) throws SQLException {
		return type.nullSafeGet( resultSet, columnAliases, context.getSession(), null );
	}
}

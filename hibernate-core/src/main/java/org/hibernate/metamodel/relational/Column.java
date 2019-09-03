/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.relational;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.sql.ast.tree.from.ColumnReferenceQualifier;

/**
 * @author Steve Ebersole
 */
public interface Column {
	String getColumnExpression();

	Table getTable();

	JdbcMapping getJdbcMapping();

	/**
	 * Render this Column's expression for reading accounting for any
	 * configured {@link ColumnTransformer#read()}
	 */
	default String renderForRead(ColumnReferenceQualifier qualifier, SessionFactoryImplementor sessionFactory) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	/**
	 * Render this Column's expression for reading accounting for any
	 * configured {@link ColumnTransformer#write()}
	 */
	default String renderForWrite(SessionFactoryImplementor sessionFactory) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}
}

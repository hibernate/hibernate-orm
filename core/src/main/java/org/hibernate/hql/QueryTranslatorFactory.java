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
package org.hibernate.hql;

import org.hibernate.engine.SessionFactoryImplementor;

import java.util.Map;

/**
 * Facade for generation of {@link QueryTranslator} and {@link FilterTranslator} instances.
 *
 * @author Gavin King
 */
public interface QueryTranslatorFactory {
	/**
	 * Construct a {@link QueryTranslator} instance capable of translating
	 * an HQL query string.
	 *
	 * @param queryIdentifier The query-identifier (used in
	 * {@link org.hibernate.stat.QueryStatistics} collection). This is
	 * typically the same as the queryString parameter except for the case of
	 * split polymorphic queries which result in multiple physical sql
	 * queries.
	 * @param queryString The query string to be translated
	 * @param filters Currently enabled filters
	 * @param factory The session factory.
	 * @return an appropriate translator.
	 */
	public QueryTranslator createQueryTranslator(String queryIdentifier, String queryString, Map filters, SessionFactoryImplementor factory);

	/**
	 * Construct a {@link FilterTranslator} instance capable of translating
	 * an HQL filter string.
	 *
	 * @see #createQueryTranslator
	 */
	public FilterTranslator createFilterTranslator(String queryIdentifier, String queryString, Map filters, SessionFactoryImplementor factory);
}

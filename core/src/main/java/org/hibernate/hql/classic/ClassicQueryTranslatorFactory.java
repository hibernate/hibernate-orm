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
package org.hibernate.hql.classic;

import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.hql.FilterTranslator;
import org.hibernate.hql.QueryTranslator;
import org.hibernate.hql.QueryTranslatorFactory;

import java.util.Map;

/**
 * Generates translators which uses the older hand-written parser to perform
 * the translation.
 *
 * @author Gavin King
 */
public class ClassicQueryTranslatorFactory implements QueryTranslatorFactory {

	/**
	 * @see QueryTranslatorFactory#createQueryTranslator
	 */
	public QueryTranslator createQueryTranslator(
			String queryIdentifier,
	        String queryString,
	        Map filters,
	        SessionFactoryImplementor factory) {
		return new QueryTranslatorImpl( queryIdentifier, queryString, filters, factory );
	}

	/**
	 * @see QueryTranslatorFactory#createFilterTranslator
	 */
	public FilterTranslator createFilterTranslator(
			String queryIdentifier,
			String queryString,
	        Map filters,
	        SessionFactoryImplementor factory) {
		return new QueryTranslatorImpl( queryIdentifier, queryString, filters, factory );
	}

}

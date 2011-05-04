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
package org.hibernate.hql.internal.classic;
import org.hibernate.QueryException;

/**
 * A parser is a state machine that accepts a string of tokens,
 * bounded by start() and end() and modifies a QueryTranslator. Parsers
 * are NOT intended to be threadsafe. They SHOULD be reuseable
 * for more than one token stream.
 */

public interface Parser {
	public void token(String token, QueryTranslatorImpl q) throws QueryException;

	public void start(QueryTranslatorImpl q) throws QueryException;

	public void end(QueryTranslatorImpl q) throws QueryException;
}








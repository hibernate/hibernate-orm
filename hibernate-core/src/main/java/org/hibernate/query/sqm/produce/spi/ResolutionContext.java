/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.spi;

import org.hibernate.query.sqm.produce.internal.FromElementBuilder;

/**
 * Defines a context for performing path resolutions
 *
 * @author Steve Ebersole
 */
public interface ResolutionContext {
	FromElementLocator getFromElementLocator();
	FromElementBuilder getFromElementBuilder();
	ParsingContext getParsingContext();
}

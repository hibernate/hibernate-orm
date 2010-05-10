/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.dialect.function;

import java.util.List;

import org.hibernate.QueryException;
import org.hibernate.engine.SessionFactoryImplementor;

/**
 * Some databases strictly return the type of the of the aggregation value for <tt>AVG</tt> which is
 * problematic in the case of averaging integers because the decimals will be dropped.  The usual workaround
 * is to cast the integer argument as some form of double/decimal.
 * <p/>
 * A downside to this approach is that we always wrap the avg() argument in a cast even though we may not need or want
 * to.  A more full-featured solution would be defining {@link SQLFunction} such that we render based on the first
 * argument; essentially have {@link SQLFunction} describe the basic metadata about the function and merge the
 * {@link SQLFunction#getReturnType} and {@link SQLFunction#render} methods into a
 *
 * @author Steve Ebersole
 */
public class AvgWithArgumentCastFunction extends AvgFunction {
	private final TemplateRenderer renderer;

	public AvgWithArgumentCastFunction(String castType) {
		renderer = new TemplateRenderer( "avg(cast(?1 as " + castType + "))" );
	}

	public String render(List args, SessionFactoryImplementor factory) throws QueryException {
		return renderer.render( args, factory );
	}
}

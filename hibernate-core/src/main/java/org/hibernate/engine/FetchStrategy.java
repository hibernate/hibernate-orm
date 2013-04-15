/*
 * jDocBook, processing of DocBook sources
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.engine;

/**
 * Describes the strategy for fetching an association
 * <p/>
 * todo not really a fan of the name.  not sure of a better one though.
 * I'd almost rather see this be called the style, but then what to call FetchStyle?  HowToFetch?
 *
 * @author Steve Ebersole
 */
public class FetchStrategy {
	private final FetchTiming timing;
	private final FetchStyle style;

	public FetchStrategy(FetchTiming timing, FetchStyle style) {
		this.timing = timing;
		this.style = style;
	}

	public FetchTiming getTiming() {
		return timing;
	}

	public FetchStyle getStyle() {
		return style;
	}
}

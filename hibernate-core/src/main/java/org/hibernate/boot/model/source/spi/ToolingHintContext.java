/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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
package org.hibernate.boot.model.source.spi;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.hibernate.mapping.MetaAttribute;

/**
 * Represents a collection of "tooling hints" ({@code <meta/>} mapping info) keyed by a name.
 * <p/>
 * NOTE : historically these were called "meta attributes", but as these are values used solely
 * by external tooling it was decided to begin calling them tooling hints.  For temporary
 * backwards compatibility (temporary until we move away from o.h.mapping model) you will
 * see mixed usage.
 *
 * @author Steve Ebersole
 */
public class ToolingHintContext {
	private final ConcurrentMap<String, ToolingHint> toolingHintMap = new ConcurrentHashMap<String, ToolingHint>();

	public ToolingHintContext(ToolingHintContext baseline) {
		if ( baseline == null ) {
			return;
		}

		for ( ToolingHint toolingHint : baseline.toolingHintMap.values() ) {
			if ( toolingHint.isInheritable() ) {
				toolingHintMap.put( toolingHint.getName(), toolingHint );
			}
		}
	}

	public Collection<ToolingHint> getToolingHints() {
		return toolingHintMap.values();
	}

	public Iterable<String> getKeys() {
		return toolingHintMap.keySet();
	}

	public ToolingHint getToolingHint(String key) {
		return toolingHintMap.get( key );
	}

	public void add(ToolingHint toolingHint) {
		toolingHintMap.put( toolingHint.getName(), toolingHint );
	}

	/**
	 * The {@link org.hibernate.mapping} package accepts these as a Map, so for now
	 * expose the underlying Map.  But we unfortunately need to collect a Map...
	 *
	 * @return The underlying Map
	 */
	public Map<String,MetaAttribute> getMetaAttributeMap() {
		final Map<String,MetaAttribute> collectedAttributeMap = new ConcurrentHashMap<String, MetaAttribute>();
		for ( ToolingHint toolingHint : toolingHintMap.values() ) {
			collectedAttributeMap.put( toolingHint.getName(), toolingHint.asMetaAttribute() );
		}
		return collectedAttributeMap;
	}
}

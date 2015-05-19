/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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

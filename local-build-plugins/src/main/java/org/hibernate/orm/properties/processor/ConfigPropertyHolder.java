/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.properties.processor;

import java.io.Writer;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class ConfigPropertyHolder {

	private final Map<String, ConfigurationProperty> properties = new TreeMap<>();


	public boolean isEmpty() {
		return properties.isEmpty();
	}

	public void write(BiConsumer<Map<String, ConfigurationProperty>, Writer> transformer, Writer writer) {
		transformer.accept( this.properties, writer );
	}

	public void put(String key, ConfigurationProperty property) {
		properties.put( key, property );
	}

	public boolean hasProperties() {
		return !properties.isEmpty();
	}

	public boolean hasProperties(Predicate<Map.Entry<String, ConfigurationProperty>> filter) {
		return properties.entrySet().stream().anyMatch( filter );
	}
}

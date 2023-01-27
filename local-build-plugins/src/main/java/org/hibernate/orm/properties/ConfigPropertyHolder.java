/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.properties;

import java.io.Writer;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class ConfigPropertyHolder {

	private final Set<ConfigurationProperty> properties = new TreeSet<>();


	public boolean isEmpty() {
		return properties.isEmpty();
	}

	public void write(BiConsumer<Set<ConfigurationProperty>, Writer> transformer, Writer writer) {
		transformer.accept( this.properties, writer );
	}

	public void add(ConfigurationProperty property) {
		properties.add( property );
	}

	public boolean hasProperties() {
		return !properties.isEmpty();
	}

	public boolean hasProperties(Predicate<ConfigurationProperty> filter) {
		return properties.stream().anyMatch( filter );
	}
}

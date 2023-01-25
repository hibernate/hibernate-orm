/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.properties.processor;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ConfigurationProperty implements Comparable<ConfigurationProperty> {

	private static final Comparator<ConfigurationProperty> CONFIGURATION_PROPERTY_COMPARATOR = Comparator.comparing(
			c -> c.key().key );
	private Key key;
	private String javadoc;
	private String sourceClass;
	private String anchorPrefix;
	private String moduleName;

	public Key key() {
		return key;
	}

	public ConfigurationProperty key(Key key) {
		this.key = key;
		return this;
	}

	public String javadoc() {
		return javadoc;
	}

	public ConfigurationProperty javadoc(String javadoc) {
		this.javadoc = javadoc == null ? "" : javadoc;
		return this;
	}

	public String sourceClass() {
		return sourceClass;
	}

	public ConfigurationProperty sourceClass(String sourceClass) {
		this.sourceClass = sourceClass;
		return this;
	}

	public String anchorPrefix() {
		return anchorPrefix;
	}

	public ConfigurationProperty withAnchorPrefix(String anchorPrefix) {
		this.anchorPrefix = anchorPrefix.replaceAll( "[^\\w-.]", "_" );
		return this;
	}

	public String moduleName() {
		return moduleName;
	}

	public ConfigurationProperty withModuleName(String moduleName) {
		this.moduleName = moduleName;
		return this;
	}

	@Override
	public String toString() {
		return "ConfigurationProperty{" +
				"key='" + key + '\'' +
				", javadoc='" + javadoc + '\'' +
				", sourceClass='" + sourceClass + '\'' +
				", anchorPrefix='" + anchorPrefix + '\'' +
				", moduleName='" + moduleName + '\'' +
				'}';
	}

	@Override
	public int compareTo(ConfigurationProperty o) {
		return CONFIGURATION_PROPERTY_COMPARATOR.compare( this, o );
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		ConfigurationProperty that = (ConfigurationProperty) o;
		return Objects.equals( key, that.key ) &&
				Objects.equals( javadoc, that.javadoc ) &&
				Objects.equals( sourceClass, that.sourceClass ) &&
				Objects.equals( anchorPrefix, that.anchorPrefix ) &&
				Objects.equals( moduleName, that.moduleName );
	}

	@Override
	public int hashCode() {
		return Objects.hash( key, javadoc, sourceClass, anchorPrefix, moduleName );
	}

	public static class Key {
		private final List<String> prefixes;
		private final String key;

		public Key(List<String> prefixes, String key) {
			this.key = key;
			this.prefixes = prefixes;
		}

		public void overridePrefixes(String... prefixes) {
			overridePrefixes( Arrays.asList( prefixes ) );
		}

		public void overridePrefixes(List<String> prefixes) {
			this.prefixes.clear();
			this.prefixes.addAll( prefixes );
		}

		public boolean matches(Pattern pattern) {
			return pattern.matcher( key ).matches();
		}

		public List<String> resolvedKeys() {
			if ( prefixes.isEmpty() ) {
				return Collections.singletonList( key );
			}
			else {
				return prefixes.stream()
						.map( p -> p + key )
						.collect( Collectors.toList() );
			}
		}

		@Override
		public String toString() {
			return toString( "/" );
		}

		private String toString(String delimiter) {
			if ( prefixes.isEmpty() ) {
				return key;
			}
			else {
				return prefixes.stream()
						.map( p -> p + key )
						.collect( Collectors.joining( delimiter ) );
			}
		}
	}
}

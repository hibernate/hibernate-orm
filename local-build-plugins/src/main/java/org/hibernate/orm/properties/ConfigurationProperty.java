/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.properties;

import java.util.Comparator;
import java.util.Objects;

public class ConfigurationProperty implements Comparable<ConfigurationProperty> {

	private static final Comparator<ConfigurationProperty> CONFIGURATION_PROPERTY_COMPARATOR = Comparator.comparing(
			ConfigurationProperty::key );
	private String key;
	private String javadoc;
	private String sourceClass;
	private String anchorPrefix;
	private String moduleName;

	public String key() {
		return key;
	}

	public ConfigurationProperty key(String key) {
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

	public ConfigurationProperty anchorPrefix(String anchorPrefix) {
		this.anchorPrefix = anchorPrefix.replaceAll( "[^\\w-.]", "_" );
		return this;
	}

	public String moduleName() {
		return moduleName;
	}

	public ConfigurationProperty moduleName(String moduleName) {
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
}

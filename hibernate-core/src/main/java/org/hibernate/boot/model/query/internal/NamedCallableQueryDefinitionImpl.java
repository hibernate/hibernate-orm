/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.model.query.internal;

import org.hibernate.boot.model.query.spi.NamedCallableQueryDefinition;

/**
 * @author Steve Ebersole
 */
public class NamedCallableQueryDefinitionImpl implements NamedCallableQueryDefinition {
	private final String name;
	// todo (6.0) : lockMode, etc

	private NamedCallableQueryDefinitionImpl(String name) {
		this.name = name;
	}

	public  static class Builder {
		private final String name;

		public Builder(String name) {
			this.name = name;
		}

		public NamedCallableQueryDefinition build() {
			return new NamedCallableQueryDefinitionImpl( name );
		}
	}
}

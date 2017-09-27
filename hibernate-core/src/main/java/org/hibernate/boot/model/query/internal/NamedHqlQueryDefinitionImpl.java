/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.model.query.internal;

import org.hibernate.boot.model.query.spi.NamedHqlQueryDefinition;

/**
 * @author Steve Ebersole
 */
public class NamedHqlQueryDefinitionImpl extends AbstractNamedQueryDefinition implements NamedHqlQueryDefinition {
	private final String hqlString;
	// todo (6.0) : lockMode, etc
	// todo (6.0) : ^^ to an abstract class

	private NamedHqlQueryDefinitionImpl(String name, String hqlString) {
		super( name );
		this.hqlString = hqlString;
	}

	@Override
	public String getQueryString() {
		return hqlString;
	}

	public  static class Builder extends AbstractBuilder<Builder> {
		private final String hqlString;

		public Builder(String name, String hqlString) {
			super( name );
			this.hqlString = hqlString;
		}

		@Override
		protected Builder getThis() {
			return this;
		}

		public NamedHqlQueryDefinitionImpl build() {
			return new NamedHqlQueryDefinitionImpl( getName(), hqlString );
		}
	}
}

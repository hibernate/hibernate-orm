/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.id.factory;

import java.util.Properties;
import java.io.Serializable;

import org.hibernate.id.IdentifierGenerator;
import org.hibernate.type.Type;
import org.hibernate.dialect.Dialect;

/**
 * Contract for a <tt>factory</tt> of {@link IdentifierGenerator} instances.
 *
 * @author Steve Ebersole
 */
public interface IdentifierGeneratorFactory {

	/**
	 * Marker object returned from {@link IdentifierGenerator#generate} to indicate that we should short-circuit any
	 * continued generated id checking.  Currently this is only used in the case of the
	 * {@link org.hibernate.id.ForeignGenerator foreign} generator as a way to signal that we should use the associated
	 * entity's id value.
	 */
	public static final Serializable SHORT_CIRCUIT_INDICATOR = new Serializable() {
		public String toString() {
			return "SHORT_CIRCUIT_INDICATOR";
		}
	};

	/**
	 * Marker object returned from {@link IdentifierGenerator#generate} to indicate that the entity's identifier will
	 * be generated as part of the datbase insertion.
	 */
	public static final Serializable POST_INSERT_INDICATOR = new Serializable() {
		public String toString() {
			return "POST_INSERT_INDICATOR";
		}
	};

	/**
	 * Allow injection of the dialect to use.
	 *
	 * @param dialect The dialect
	 * @deprecated The intention is that Dialect should be required to be specified up-front and it would then get
	 * ctor injected.
	 */
	public void setDialect(Dialect dialect);

	/**
	 * Given a strategy, retrieve the appropriate identifier generator instance.
	 *
	 * @param strategy The generation strategy.
	 * @param type The mapping type for the identifier values.
	 * @param config Any configuraion properties given in the generator mapping.
	 *
	 * @return The appropriate generator instance.
	 */
	public IdentifierGenerator createIdentifierGenerator(String strategy, Type type, Properties config);

	/**
	 * Retrieve the class that will be used as the {@link IdentifierGenerator} for the given strategy.
	 *
	 * @param strategy The strategy
	 * @return The generator class.
	 */
	public Class getIdentifierGeneratorClass(String strategy);
}

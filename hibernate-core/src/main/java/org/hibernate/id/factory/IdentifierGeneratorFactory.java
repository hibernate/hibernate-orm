/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id.factory;

import java.util.Properties;

import org.hibernate.dialect.Dialect;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * Contract for a <tt>factory</tt> of {@link IdentifierGenerator} instances.
 *
 * @author Steve Ebersole
 */
public interface IdentifierGeneratorFactory {
	/**
	 * Get the dialect.
	 *
	 * @return the dialect
	 */
	Dialect getDialect();

	/**
	 * Allow injection of the dialect to use.
	 *
	 * @param dialect The dialect
	 *
	 * @deprecated The intention is that Dialect should be required to be specified up-front and it would then get
	 * ctor injected.
	 */
	@Deprecated
	void setDialect(Dialect dialect);

	/**
	 * Given a strategy, retrieve the appropriate identifier generator instance.
	 *
	 * @param strategy The generation strategy.
	 * @param jtd The descriptor for the identifier's Java type.
	 * @param config Any configuration properties given in the generator mapping.
	 *
	 * @return The appropriate generator instance.
	 */
	IdentifierGenerator createIdentifierGenerator(String strategy, JavaTypeDescriptor jtd, Properties config);

	/**
	 * Retrieve the class that will be used as the {@link IdentifierGenerator} for the given strategy.
	 *
	 * @param strategy The strategy
	 * @return The generator class.
	 */
	Class getIdentifierGeneratorClass(String strategy);
}

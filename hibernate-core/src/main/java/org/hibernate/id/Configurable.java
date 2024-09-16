/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id;

import java.util.Properties;

import org.hibernate.Incubating;
import org.hibernate.MappingException;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.generator.GeneratorCreationContext;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.Type;

/**
 * A {@link org.hibernate.generator.Generator} that supports "configuration".
 *
 * @apiNote Prefer instead using either<ul>
 *     <li>{@linkplain org.hibernate.annotations.IdGeneratorType}</li>, or
 *     <li>{@linkplain org.hibernate.generator.AnnotationBasedGenerator} (in either implementation or constructor-injection form)</li>
 * </ul>
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public interface Configurable {
	/**
	 * Called before {@link #configure(Type, Properties, ServiceRegistry)},
	 * with an instance of {@link GeneratorCreationContext}.
	 *
	 * @since 6.6
	 * @deprecated Added in 6.6 as a short-term work-around, but should really use on
	 * of the alternatives discussed at {@linkplain Configurable}.
	 * See <a href="https://hibernate.atlassian.net/browse/HHH-18615">HHH-18615</a>.
	 */
	@Deprecated(forRemoval = true)
	default void create(GeneratorCreationContext creationContext) throws MappingException {}

	/**
	 * Configure this instance, given the value of parameters
	 * specified by the user as XML {@code <param>} elements and
	 * {@link org.hibernate.annotations.Parameter @Parameter}
	 * annotations.
	 * <p>
	 * This method is called just once, following instantiation.
	 * If this instance also implements {@code ExportableProducer},
	 * then this method is always called before
	 * {@link org.hibernate.boot.model.relational.ExportableProducer#registerExportables(Database)},
	 *
	 * @param type The id property type descriptor
	 * @param parameters param values, keyed by parameter name
	 * @param serviceRegistry Access to service that may be needed.
	 *
	 * @throws MappingException when there's something wrong with the given parameters
	 */
	void configure(Type type, Properties parameters, ServiceRegistry serviceRegistry) throws MappingException;

	/**
	 * Initializes this instance, pre-generating SQL if necessary.
	 * <p>
	 * If this instance also implements {@code ExportableProducer},
	 * then this method is always called after
	 * {@link org.hibernate.boot.model.relational.ExportableProducer#registerExportables(Database)},
	 * and before first use.
	 *
	 * @param context A context to help generate SQL strings
	 */
	default void initialize(SqlStringGenerationContext context) {}
}

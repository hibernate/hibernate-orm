/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id;

import java.util.Properties;

import org.hibernate.MappingException;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * An {@link IdentifierGenerator} that supports "configuration".
 *
 * @see IdentifierGenerator
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public interface Configurable {
	/**
	 * Configure this instance, given the value of parameters
	 * specified by the user as <tt>&lt;param&gt;</tt> elements.
	 * This method is called just once, following instantiation.
	 *
	 * @param javaTypeDescriptor The id property java type descriptor
	 * @param params param values, keyed by parameter name
	 * @param serviceRegistry Access to service that may be needed.
	 */
	void configure(JavaTypeDescriptor javaTypeDescriptor, Properties params, ServiceRegistry serviceRegistry) throws MappingException;
}

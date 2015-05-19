/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.spi;

import org.hibernate.MappingException;
import org.hibernate.id.factory.IdentifierGeneratorFactory;
import org.hibernate.type.Type;

/**
 * Defines operations common to "compiled" mappings (ie. <tt>SessionFactory</tt>)
 * and "uncompiled" mappings (ie. <tt>Configuration</tt>) that are used by
 * implementors of <tt>Type</tt>.
 *
 * @see org.hibernate.type.Type
 * @see org.hibernate.internal.SessionFactoryImpl
 * @see org.hibernate.cfg.Configuration
 * @author Gavin King
 */
public interface Mapping {
	/**
	 * Allow access to the id generator factory, though this is only needed/allowed from configuration.
	 *
	 * @return Access to the identifier generator factory
	 *
	 * @deprecated temporary solution 
	 */
	@Deprecated
	public IdentifierGeneratorFactory getIdentifierGeneratorFactory();

	public Type getIdentifierType(String className) throws MappingException;
	public String getIdentifierPropertyName(String className) throws MappingException;
	public Type getReferencedPropertyType(String className, String propertyName) throws MappingException;
}

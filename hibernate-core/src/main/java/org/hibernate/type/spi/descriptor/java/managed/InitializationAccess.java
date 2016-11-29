/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.spi.descriptor.java.managed;

/**
 * Defines access to initialization for state pertaining to a managed (non-basic) type.
 *
 * @author Steve Ebersole
 */
public interface InitializationAccess {
	void setJavaType(Class javaType);
	void setSuperType(JavaTypeDescriptorManagedImplementor superType);

	// todo : some kind of "site context" (ala org.hibernate.boot.model.type.spi.BasicTypeSiteContext)
	//		rather than repetitive setters called from externally.
	//		instead have those external sites inject "site context" here which we can use for resolving this managed type state

	AttributeBuilderSingular getSingularAttributeBuilder(String name);
	AttributeBuilderPlural getPluralAttributeBuilder(String name);

	/**
	 * To be called after all initialization work is done - as we transition into a
	 * working SessionFactory.  After this method is called, access to the
	 * InitializationAccess nor any of its methods should not be allowed.
	 */
	void complete();
}

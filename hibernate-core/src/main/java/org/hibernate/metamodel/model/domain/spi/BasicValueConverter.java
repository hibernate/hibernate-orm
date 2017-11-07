/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * Models the common ways that a basic value can be "converted".  This
 * can be through:
 *
 * 		* a custom JPA {@link javax.persistence.AttributeConverter},
 * 		* implicitly, based on the Java type (e.g., enums)
 * 		* normal wrap/unwrap via {@link org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor}
 *
 * @author Steve Ebersole
 */
public interface BasicValueConverter<O,R> {
	O toDomainValue(R relationalForm, SharedSessionContractImplementor session);
	R toRelationalValue(O domainForm, SharedSessionContractImplementor session);
}

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tuple.entity;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.MappedSuperclass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.property.access.spi.Setter;
import org.hibernate.proxy.ProxyFactory;

/**
 * @author Andrea Boriero
 */
public interface ProxyBuilder {
	ProxyFactory buildProxyFactory(
			MappedSuperclass mappedSuperclass,
			Getter idGetter,
			Setter idSetter,
			SessionFactoryImplementor sessionFactory);

	ProxyFactory buildProxyFactory(
			PersistentClass persistentClass,
			String entityName,
			Getter idGetter,
			Setter idSetter,
			SessionFactoryImplementor sessionFactory);
}

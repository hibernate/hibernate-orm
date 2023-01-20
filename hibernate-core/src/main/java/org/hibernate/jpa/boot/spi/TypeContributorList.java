/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.boot.spi;

import java.util.List;

import org.hibernate.boot.model.TypeContributor;

/**
 * An object that provides a list of {@link TypeContributor}s to the JPA persistence provider.
 * <p>
 * An implementation may be registered with the JPA provider using the property
 * {@value org.hibernate.jpa.boot.spi.JpaSettings#TYPE_CONTRIBUTORS}.
 *
 * @deprecated Consider using {@linkplain java.util.ServiceLoader discovery} instead to
 * dynamically locate {@linkplain TypeContributor contributors}.
 *
 * @author Brett Meyer
 */
@Deprecated(forRemoval = true)
public interface TypeContributorList {
	List<TypeContributor> getTypeContributors();
}

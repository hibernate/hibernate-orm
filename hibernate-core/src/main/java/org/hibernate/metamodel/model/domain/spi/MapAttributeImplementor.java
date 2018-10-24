/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import java.util.Map;
import javax.persistence.metamodel.MapAttribute;

/**
 * Hibernate extension to the JPA {@link MapAttribute} descriptor
 *
 * @author Steve Ebersole
 */
public interface MapAttributeImplementor<D,K,V> extends MapAttribute<D, K, V>, PluralAttributeImplementor<D,Map<K,V>,V> {
	@Override
	SimpleTypeImplementor<K> getKeyType();
}

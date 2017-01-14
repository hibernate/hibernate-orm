/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.common.spi;

import java.util.Map;
import javax.persistence.metamodel.MapAttribute;

/**
 * @author Steve Ebersole
 */
public interface MapAttribute<O,K,E> extends OrmPluralAttribute<O,Map<K,E>,E>,
		javax.persistence.metamodel.MapAttribute<O,K,E> {
}

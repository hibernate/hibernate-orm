/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.queryable.spi;

import org.hibernate.persister.common.spi.Navigable;

/**
 * Specialization for Navigable that can be used in creating TableGroupJoins
 *
 * @author Steve Ebersole
 */
public interface Joinable<T> extends Navigable<T>, TableGroupJoinProducer {

}

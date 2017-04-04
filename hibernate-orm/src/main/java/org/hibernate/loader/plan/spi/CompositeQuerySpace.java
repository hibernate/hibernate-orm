/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.plan.spi;

/**
 * Models a QuerySpace for a composition (component/embeddable).
 * <p/>
 * It's {@link #getDisposition()} result will be {@link Disposition#COMPOSITE}
 *
 * @author Steve Ebersole
 */
public interface CompositeQuerySpace extends QuerySpace {
}

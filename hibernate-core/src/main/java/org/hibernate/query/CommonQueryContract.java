/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query;

/**
 * Defines the aspects of query definition that apply to all forms of
 * querying (HQL, JPQL, criteria) across all forms of persistence contexts
 * (Session, StatelessSession, EntityManager).
 *
 * @author Steve Ebersole
 * @author Gavin King
 */
public interface CommonQueryContract extends org.hibernate.BasicQueryContract {
}

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa;

import org.hibernate.Transaction;

/**
 * Encapsulates settings controlling whether certain aspects of the JPA spec
 * should be strictly followed.
 *
 * @author Steve Ebersole
 */
public interface JpaCompliance {
	/**
	 * Controls whether Hibernate's handling of JPA's
	 * {@link javax.persistence.Query} (JPQL, Criteria and native-query) should
	 * strictly follow the JPA spec.  This includes both in terms of parsing or
	 * translating a query as well as calls to the {@link javax.persistence.Query}
	 * methods throwing spec defined exceptions where as Hibernate might not.
	 *
	 * Deviations result in an exception if enabled
	 *
	 * @return {@code true} indicates to behave in the spec-defined way
	 */
	boolean isJpaQueryComplianceEnabled();

	/**
	 * Indicates that Hibernate's {@link Transaction} should behave as
	 * defined by the spec for JPA's {@link javax.persistence.EntityTransaction}
	 * since it extends the JPA one.
	 *
	 * @return {@code true} indicates to behave in the spec-defined way
	 */
	boolean isJpaTransactionComplianceEnabled();

	/**
	 * Controls how Hibernate interprets a mapped List without an "order columns"
	 * specified.  Historically Hibernate defines this as a "bag", which is a concept
	 * JPA does not have.
	 *
	 * If enabled, Hibernate will recognize this condition as defining
	 * a {@link org.hibernate.collection.internal.PersistentList}, otherwise
	 * Hibernate will treat is as a {@link org.hibernate.collection.internal.PersistentBag}
	 *
	 * @return {@code true} indicates to behave in the spec-defined way, interpreting the
	 * mapping as a "list", rather than a "bag"
	 */
	boolean isJpaListComplianceEnabled();

	/**
	 *JPA defines specific exceptions on specific methods when called on
	 * {@link javax.persistence.EntityManager} and {@link javax.persistence.EntityManagerFactory}
	 * when those objects have been closed.  This setting controls
	 * whether the spec defined behavior or Hibernate's behavior will be used.
	 *
	 * If enabled Hibernate will operate in the JPA specified way throwing
	 * exceptions when the spec says it should with regard to close checking
	 *
	 * @return {@code true} indicates to behave in the spec-defined way
	 */
	boolean isJpaClosedComplianceEnabled();
}

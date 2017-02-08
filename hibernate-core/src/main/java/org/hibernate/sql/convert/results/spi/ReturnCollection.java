/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.convert.results.spi;

/**
 * Models the a persistent collection as root {@link Return}.  Pertinent to collection initializer
 * ({@link org.hibernate.loader.plan.spi.LoadPlan.Disposition#COLLECTION_INITIALIZER}) LoadPlans only,
 *
 * @author Steve Ebersole
 */
public interface ReturnCollection extends CollectionReference, Return {
}

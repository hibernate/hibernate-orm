/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.plan.spi;

/**
 * Models the an entity as root {@link Return}.  Pertinent to entity loader
 * ({@link org.hibernate.loader.plan.spi.LoadPlan.Disposition#ENTITY_LOADER}) and mixed
 * ({@link org.hibernate.loader.plan.spi.LoadPlan.Disposition#MIXED}) LoadPlans
 *
 * @author Steve Ebersole
 */
public interface EntityReturn extends EntityReference, Return {
}

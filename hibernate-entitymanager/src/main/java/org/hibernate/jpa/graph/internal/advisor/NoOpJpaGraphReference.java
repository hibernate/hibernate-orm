/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.jpa.graph.internal.advisor;

import org.hibernate.loader.plan.spi.FetchOwner;

/**
 * A no-op implementation of JpaGraphReference.  Used when the LoadPlan graph already defines
 * nodes beyond the scope of the JPA graph.
 *
 * @author Steve Ebersole
 */
class NoOpJpaGraphReference implements JpaGraphReference {
	public static final NoOpJpaGraphReference INSTANCE = new NoOpJpaGraphReference();

	@Override
	public JpaGraphReference attributeProcessed(String attributeName) {
		// its no-op, nothing to do
		return INSTANCE;
	}

	@Override
	public void applyMissingFetches(FetchOwner fetchOwner) {
		// its no-op, nothing to do
	}
}

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
package org.hibernate.tuple;

/**
 * @author Steve Ebersole
 */
public interface InMemoryValueGenerationStrategy {
	/**
	 * When is this value generated : NEVER, INSERT, ALWAYS (INSERT+UPDATE)
	 *
	 * @return When the value is generated.
	 */
	public GenerationTiming getGenerationTiming();

	/**
	 * Obtain the in-VM value generator.
	 * <p/>
	 * May return {@code null}.  In fact for values that are generated "in the database" via execution of the
	 * INSERT/UPDATE statement, the expectation is that {@code null} be returned here
	 *
	 * @return The strategy for performing in-VM value generation
	 */
	public ValueGenerator getValueGenerator();
}

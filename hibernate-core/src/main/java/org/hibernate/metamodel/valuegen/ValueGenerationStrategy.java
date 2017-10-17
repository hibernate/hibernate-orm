/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.valuegen;

import org.hibernate.tuple.GenerationTiming;

/**
 * Unifying contract for both in-memory and in-database generation strategies
 *
 * @author Steve Ebersole
 */
public interface ValueGenerationStrategy {
	/**
	 * When is this value generated : NEVER, INSERT, ALWAYS (INSERT+UPDATE)
	 */
	GenerationTiming getGenerationTiming();
}

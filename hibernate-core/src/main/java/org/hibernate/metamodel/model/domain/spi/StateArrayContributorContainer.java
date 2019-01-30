/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author Steve Ebersole
 */
public interface StateArrayContributorContainer {
	List<StateArrayContributor<?>> getStateArrayContributors();

	default void visitStateArrayContributors(Consumer<StateArrayContributor<?>> consumer) {
		for ( StateArrayContributor contributor : getStateArrayContributors() ) {
			consumer.accept( contributor );
		}
	}

	default <T> Collection<T> visitAndCollectStateArrayContributors(Function<StateArrayContributor<?>,T> function) {
		List<T> results = new ArrayList<>();
		for ( StateArrayContributor contributor : getStateArrayContributors() ) {
			results.add( function.apply( contributor ) );
		}
		return results;
	}
}

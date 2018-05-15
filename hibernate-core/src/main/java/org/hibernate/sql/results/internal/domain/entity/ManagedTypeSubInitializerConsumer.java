/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal.domain.entity;

import java.util.function.Consumer;

import org.hibernate.sql.results.spi.Initializer;

/**
 * @author Steve Ebersole
 */
public class ManagedTypeSubInitializerConsumer implements Consumer<Initializer> {
	private final Consumer<Initializer> fallbackConsumer;
//	private List<Initializer> postRegistrationInitializers;

	public ManagedTypeSubInitializerConsumer(Consumer<Initializer> fallbackConsumer) {
		this.fallbackConsumer = fallbackConsumer;
	}

	@Override
	public void accept(Initializer initializer) {
//		if ( initializer instanceof CollectionInitializer ) {
//			if ( postRegistrationInitializers == null ) {
//				postRegistrationInitializers = new ArrayList<>();
//			}
//			postRegistrationInitializers.add( initializer );
//		}
//		else {
			fallbackConsumer.accept( initializer );
//		}
	}

	public void finishUp() {
//		if ( postRegistrationInitializers == null ) {
//			return;
//		}
//
//		postRegistrationInitializers.forEach( fallbackConsumer );
	}
}

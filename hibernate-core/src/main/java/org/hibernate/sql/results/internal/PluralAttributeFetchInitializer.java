/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal;

import org.hibernate.metamodel.model.domain.spi.PluralPersistentAttribute;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.sql.results.spi.PluralAttributeInitializer;
import org.hibernate.sql.results.spi.JdbcValuesSourceProcessingState;
import org.hibernate.sql.results.spi.RowProcessingState;

/**
 * @author Steve Ebersole
 */
public class PluralAttributeFetchInitializer implements PluralAttributeInitializer {
	private final PluralPersistentAttribute fetchedAttribute;

	public PluralAttributeFetchInitializer(PluralPersistentAttribute fetchedAttribute) {
		this.fetchedAttribute = fetchedAttribute;
	}

	@Override
	public PluralPersistentAttribute getFetchedAttribute() {
		return fetchedAttribute;
	}

	@Override
	public void finishUpRow(RowProcessingState rowProcessingState) {

	}

	@Override
	public void endLoading(JdbcValuesSourceProcessingState processingState) {
		// complete loading for all "loading collection" references

		// todo (6.0) : determine how this should work in 6.0
		//		One possibility is to keep track of something like
		// 		CollectionLoadContext/LoadingCollectionEntry as part of
		//		JdbcValuesSourceProcessingState.
		//
		//		Another option (I think) is to keep track of them here.  This
		// 		is the thing doing the loading of each collection instance, it
		//		could keep track of them here.  Proper key is the worry.  Each
		// 		collection instance needs to be tied to a specific owner/container
		//		instance.

		throw new NotYetImplementedFor6Exception(  );
	}
}

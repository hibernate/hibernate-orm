/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.sql.results.spi.FetchParentAccess;
import org.hibernate.sql.results.spi.CompositeInitializer;
import org.hibernate.sql.results.spi.RowProcessingState;
import org.hibernate.sql.results.spi.CompositeSqlSelectionMappings;

/**
 * @author Steve Ebersole
 */
public class CompositeInitializerImpl implements CompositeInitializer {
	private final FetchParentAccess parentAccess;
	private final CompositeSqlSelectionMappings sqlSelectionMappings;

	private Object componentInstance;

	public CompositeInitializerImpl(FetchParentAccess parentAccess, CompositeSqlSelectionMappings sqlSelectionMappings) {
		this.parentAccess = parentAccess;
		this.sqlSelectionMappings = sqlSelectionMappings;
	}

	@Override
	public Object getComponentInstance() {
		return componentInstance;
	}

	@Override
	public void resolveInstance(RowProcessingState rowProcessingState) {
		throw new NotYetImplementedFor6Exception(  );
	}

	@Override
	public void finishUpRow(RowProcessingState rowProcessingState) {
		componentInstance = null;
	}
}

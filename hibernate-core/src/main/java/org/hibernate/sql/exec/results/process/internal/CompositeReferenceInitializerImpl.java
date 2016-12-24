/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.exec.results.process.internal;

import org.hibernate.sql.exec.results.process.spi.RowProcessingState;
import org.hibernate.sql.exec.results.process.spi.CompositeReferenceInitializer;
import org.hibernate.sql.exec.results.process.spi.InitializerParent;

/**
 * @author Steve Ebersole
 */
public class CompositeReferenceInitializerImpl
		extends AbstractFetchParentInitializer
		implements CompositeReferenceInitializer {
	public CompositeReferenceInitializerImpl(InitializerParent initializerParent) {
		super( initializerParent );
	}

	@Override
	public void link(Object fkValue) {

	}

	@Override
	public void finishUpRow(RowProcessingState rowProcessingState) {

	}
}

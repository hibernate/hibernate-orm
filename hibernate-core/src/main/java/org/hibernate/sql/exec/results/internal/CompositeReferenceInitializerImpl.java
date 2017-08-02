/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.exec.results.internal;

import org.hibernate.sql.exec.results.spi.RowProcessingState;
import org.hibernate.sql.exec.results.spi.InitializerComposite;
import org.hibernate.sql.exec.results.spi.InitializerParent;

/**
 * @author Steve Ebersole
 */
public class CompositeReferenceInitializerImpl implements InitializerComposite {
	public CompositeReferenceInitializerImpl(InitializerParent initializerParent) {
		super( initializerParent );
	}

	@Override
	public Object getParentInstance() {
		return null;
	}

	@Override
	public void link(Object fkValue) {

	}

	@Override
	public void finishUpRow(RowProcessingState rowProcessingState) {

	}
}

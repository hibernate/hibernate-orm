/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.consume.results.internal;

import java.util.Map;

import org.hibernate.persister.common.spi.PersistentAttribute;
import org.hibernate.sql.NotYetImplementedException;
import org.hibernate.sql.ast.produce.result.spi.FetchEntityAttribute;
import org.hibernate.sql.ast.consume.results.spi.FetchInitializer;
import org.hibernate.sql.ast.consume.results.spi.InitializerParent;
import org.hibernate.sql.ast.consume.results.spi.SqlSelectionGroup;

/**
 * @author Steve Ebersole
 */
public class EntityFetchInitializerImpl extends AbstractEntityReferenceInitializer implements FetchInitializer {
	public EntityFetchInitializerImpl(
			InitializerParent parent,
			FetchEntityAttribute entityReference,
			Map<PersistentAttribute,SqlSelectionGroup> sqlSelectionGroupMap,
			boolean isShallow) {
		super( parent, entityReference, false, sqlSelectionGroupMap, isShallow );
	}

	@Override
	protected boolean shouldBatchFetch() {
		// todo : add this method to SingularAttributeEntity
		//return !getEntityReference().getFetchedAttributeDescriptor().isReferenceToNonPk();
		return true;
	}

	@Override
	public void link(Object fkValue) {
		throw new NotYetImplementedException(  );
	}
}

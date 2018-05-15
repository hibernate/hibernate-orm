/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal.domain.embedded;

import java.util.Map;

import org.hibernate.annotations.Remove;
import org.hibernate.metamodel.model.domain.spi.EmbeddedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.StateArrayContributor;
import org.hibernate.metamodel.model.domain.spi.StateArrayContributorContainer;
import org.hibernate.sql.ast.produce.spi.ColumnReferenceQualifier;
import org.hibernate.sql.results.internal.AbstractSqlSelectionGroup;
import org.hibernate.sql.results.spi.CompositeSqlSelectionGroup;
import org.hibernate.sql.results.spi.AssemblerCreationContext;
import org.hibernate.sql.results.spi.SqlSelectionGroupNode;

/**
 * @author Steve Ebersole
 */
public class CompositeSqlSelectionGroupImpl extends AbstractSqlSelectionGroup implements CompositeSqlSelectionGroup {

	private final EmbeddedTypeDescriptor<?> embeddedDescriptor;

	@Remove
	public static CompositeSqlSelectionGroup buildSqlSelectionGroup(
			EmbeddedTypeDescriptor<?> embeddedDescriptor,
			ColumnReferenceQualifier qualifier,
			AssemblerCreationContext resolutionContext) {

		// todo (6.0) : remove - handled differently now

//		final Map<StateArrayContributor<?>, SqlSelectionGroupNode> selectionNodesByContributor = new HashMap<>();
//
//		for ( StateArrayContributor<?> stateArrayContributor : embeddedDescriptor.getStateArrayContributors() ) {
//			selectionNodesByContributor.put(
//					stateArrayContributor,
//					stateArrayContributor.resolveSqlSelections( qualifier, resolutionContext )
//			);
//		}
//
//		return new CompositeSqlSelectionGroupImpl( embeddedDescriptor, selectionNodesByContributor );
		throw new UnsupportedOperationException(  );
	}

	public CompositeSqlSelectionGroupImpl(
			EmbeddedTypeDescriptor<?> embeddedDescriptor,
			Map<StateArrayContributor<?>, SqlSelectionGroupNode> selectionNodesByContributor) {
		super( selectionNodesByContributor );
		this.embeddedDescriptor = embeddedDescriptor;
	}

	@Override
	protected StateArrayContributorContainer getContributorContainer() {
		return embeddedDescriptor;
	}
}

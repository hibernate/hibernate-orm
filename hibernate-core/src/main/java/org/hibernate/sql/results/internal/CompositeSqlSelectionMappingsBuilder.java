/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal;

import java.util.Map;

import org.hibernate.metamodel.model.domain.internal.BasicSingularPersistentAttribute;
import org.hibernate.metamodel.model.domain.internal.SingularPersistentAttributeEmbedded;
import org.hibernate.metamodel.model.domain.internal.SingularPersistentAttributeEntity;
import org.hibernate.metamodel.model.domain.spi.EmbeddedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.NavigableVisitationStrategy;
import org.hibernate.metamodel.model.domain.spi.PersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.PluralPersistentAttribute;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.sql.results.spi.QueryResultCreationContext;
import org.hibernate.sql.results.spi.SqlSelectionGroup;
import org.hibernate.sql.results.spi.CompositeSqlSelectionMappings;

/**
 * @author Steve Ebersole
 */
public class CompositeSqlSelectionMappingsBuilder implements NavigableVisitationStrategy {
	public static CompositeSqlSelectionMappings generateMappings(
			EmbeddedTypeDescriptor embeddedDescriptor,
			QueryResultCreationContext creationContext) {
		final CompositeSqlSelectionMappingsBuilder builder = new CompositeSqlSelectionMappingsBuilder( creationContext );
		embeddedDescriptor.visitNavigables( builder );
		return builder.generateMappings();
	}

	private final QueryResultCreationContext creationContext;

	private Map<PersistentAttribute,SqlSelectionGroup> attributeSqlSelectionGroupMap;

	protected CompositeSqlSelectionMappingsBuilder(QueryResultCreationContext creationContext) {
		this.creationContext = creationContext;
	}

	public CompositeSqlSelectionMappings generateMappings() {
		return new CompositeSqlSelectionMappingsImpl( attributeSqlSelectionGroupMap );
	}

	// todo (6.0) : build the `attributeSqlSelectionGroupMap`

	@Override
	public void visitSingularAttributeBasic(BasicSingularPersistentAttribute attribute) {
		throw new NotYetImplementedFor6Exception(  );
	}

	@Override
	public void visitSingularAttributeEmbedded(SingularPersistentAttributeEmbedded attribute) {
		throw new NotYetImplementedFor6Exception(  );
	}

	@Override
	public void visitSingularAttributeEntity(SingularPersistentAttributeEntity attribute) {
		throw new NotYetImplementedFor6Exception(  );
	}

	@Override
	public void visitPluralAttribute(PluralPersistentAttribute attribute) {
		throw new NotYetImplementedFor6Exception(  );
	}
}

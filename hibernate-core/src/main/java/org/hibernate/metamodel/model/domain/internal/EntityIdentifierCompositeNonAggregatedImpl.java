/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import java.util.List;

import org.hibernate.metamodel.model.domain.spi.AbstractSingularPersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.EmbeddedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityHierarchy;
import org.hibernate.metamodel.model.domain.spi.EntityIdentifierCompositeNonAggregated;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.model.domain.spi.NavigableVisitationStrategy;
import org.hibernate.metamodel.model.domain.spi.SingularPersistentAttribute;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.property.access.internal.PropertyAccessStrategyEmbeddedImpl;
import org.hibernate.sql.exec.results.spi.QueryResult;
import org.hibernate.sql.exec.results.spi.QueryResultCreationContext;
import org.hibernate.sql.exec.results.spi.SqlSelectionResolver;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class EntityIdentifierCompositeNonAggregatedImpl<O,J>
		extends AbstractSingularPersistentAttribute<O,J>
		implements EntityIdentifierCompositeNonAggregated<O,J> {
	// todo : IdClass handling eventually

	public static final String NAVIGABLE_NAME = "{id}";

	private final EmbeddedTypeDescriptor<J> embeddedDescriptor;

	@SuppressWarnings("unchecked")
	public EntityIdentifierCompositeNonAggregatedImpl(
			EntityHierarchy entityHierarchy,
			EmbeddedTypeDescriptor<J> embeddedDescriptor) {
		super(
				entityHierarchy.getRootEntityType(),
				NAVIGABLE_NAME,
				PropertyAccessStrategyEmbeddedImpl.INSTANCE.buildPropertyAccess( null, NAVIGABLE_NAME ),
				embeddedDescriptor,
				Disposition.ID,
				false
		);
		this.embeddedDescriptor = embeddedDescriptor;
	}

	@Override
	public EmbeddedTypeDescriptor getEmbeddedDescriptor() {
		return embeddedDescriptor;
	}

	@Override
	public List<Navigable> getNavigables() {
		return null;
	}

	@Override
	public List<Navigable> getDeclaredNavigables() {
		return null;
	}

	@Override
	public List<Column> getColumns() {
		return embeddedDescriptor.collectColumns();
	}

	@Override
	public boolean hasSingleIdAttribute() {
		return false;
	}

	@Override
	public PersistentAttributeType getPersistentAttributeType() {
		return PersistentAttributeType.EMBEDDED;
	}

	@Override
	public SingularPersistentAttribute<O,J> getIdAttribute() {
		return this;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SingularAttributeImplementor

	@Override
	public SingularAttributeClassification getAttributeTypeClassification() {
		return SingularAttributeClassification.EMBEDDED;
	}

	@Override
	public NavigableRole getNavigableRole() {
		return embeddedDescriptor.getNavigableRole();
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return embeddedDescriptor.getJavaTypeDescriptor();
	}

	@Override
	public String getAttributeName() {
		return NAVIGABLE_NAME;
	}

	@Override
	public String asLoggableText() {
		return "IdentifierCompositeNonAggregated(" + getContainer().asLoggableText() + ")";
	}

	@Override
	public <N> Navigable<N> findNavigable(String navigableName) {
		return embeddedDescriptor.findNavigable( navigableName );
	}

	@Override
	public <N> Navigable<N> findDeclaredNavigable(String navigableName) {
		return embeddedDescriptor.findDeclaredNavigable( navigableName );
	}

	@Override
	public void visitNavigable(NavigableVisitationStrategy visitor) {
		visitor.visitNonAggregateCompositeIdentifier( this );
	}

	@Override
	public void visitNavigables(NavigableVisitationStrategy visitor) {
		embeddedDescriptor.visitNavigables( visitor );
	}

	@Override
	public void visitDeclaredNavigables(NavigableVisitationStrategy visitor) {
		embeddedDescriptor.visitDeclaredNavigables( visitor );
	}

	@Override
	public PersistenceType getPersistenceType() {
		return PersistenceType.EMBEDDABLE;
	}

	@Override
	public QueryResult generateQueryResult(
			NavigableReference selectedExpression,
			String resultVariable,
			SqlSelectionResolver sqlSelectionResolver,
			QueryResultCreationContext creationContext) {
		return embeddedDescriptor.generateQueryResult(
				selectedExpression,
				resultVariable,
				sqlSelectionResolver,
				creationContext
		);
	}
}

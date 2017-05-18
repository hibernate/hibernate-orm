/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import java.util.List;

import org.hibernate.mapping.Property;
import org.hibernate.metamodel.model.domain.spi.NavigableRole;
import org.hibernate.metamodel.model.domain.spi.AbstractSingularPersistentAttribute;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.metamodel.model.domain.spi.NavigableContainer;
import org.hibernate.metamodel.model.domain.spi.NavigableVisitationStrategy;
import org.hibernate.metamodel.model.domain.spi.SingularPersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.EmbeddedTypeImplementor;
import org.hibernate.metamodel.model.domain.spi.EntityHierarchy;
import org.hibernate.metamodel.model.domain.spi.EntityIdentifier;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.sql.ast.produce.result.spi.QueryResult;
import org.hibernate.sql.ast.produce.result.spi.QueryResultCreationContext;
import org.hibernate.sql.ast.produce.result.spi.SqlSelectionResolver;
import org.hibernate.sql.ast.tree.spi.expression.domain.ColumnReferenceSource;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;
import org.hibernate.type.descriptor.java.spi.EmbeddableJavaDescriptor;
import org.hibernate.type.spi.EmbeddedType;

/**
 * @author Steve Ebersole
 */
public class EntityIdentifierCompositeAggregated<O,J>
		extends AbstractSingularPersistentAttribute<O,J,EmbeddedType<J>>
		implements EntityIdentifier<O,J>, SingularPersistentAttribute<O,J>, NavigableContainer<J> {
	private final EntityHierarchy entityHierarchy;
	private final EmbeddedTypeImplementor<J> embeddedPersister;

	@SuppressWarnings("unchecked")
	public EntityIdentifierCompositeAggregated(
			EntityHierarchy entityHierarchy,
			Property idAttribute,
			EmbeddedTypeImplementor<J> embeddedPersister,
			RuntimeModelCreationContext creationContext) {
		super(
				entityHierarchy.getRootEntityType(),
				idAttribute.getName(),
				PersisterHelper.resolvePropertyAccess( entityHierarchy.getRootEntityType(), idAttribute, creationContext ),
				embeddedPersister,
				Disposition.ID,
				false
		);
		this.entityHierarchy = entityHierarchy;
		this.embeddedPersister = embeddedPersister;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// NavigableSource (embedded)


	@Override
	public NavigableRole getNavigableRole() {
		return embeddedPersister.getNavigableRole();
	}

	@Override
	public EmbeddableJavaDescriptor getJavaTypeDescriptor() {
		return embeddedPersister.getJavaTypeDescriptor();
	}

	@Override
	public <N> Navigable<N> findNavigable(String navigableName) {
		return embeddedPersister.findNavigable( navigableName );
	}

	@Override
	public <N> Navigable<N> findDeclaredNavigable(String navigableName) {
		return embeddedPersister.findDeclaredNavigable( navigableName );
	}

	@Override
	public void visitNavigable(NavigableVisitationStrategy visitor) {
		visitor.visitAggregatedCompositeIdentifier( entityHierarchy, embeddedPersister );
	}

	@Override
	public void visitNavigables(NavigableVisitationStrategy visitor) {
		embeddedPersister.visitNavigables( visitor );
	}

	@Override
	public void visitDeclaredNavigables(NavigableVisitationStrategy visitor) {
		embeddedPersister.visitDeclaredNavigables( visitor );
	}



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// IdentifierDescriptor

	@Override
	public EmbeddedType getIdType() {
		return (EmbeddedType) getOrmType();
	}

	@Override
	public boolean hasSingleIdAttribute() {
		return true;
	}

	@Override
	public SingularPersistentAttribute<O,J> getIdAttribute() {
		return this;
	}

	@Override
	public List<Column> getColumns() {
		return embeddedPersister.collectColumns();
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SingularAttribute


	@Override
	public PersistentAttributeType getPersistentAttributeType() {
		return PersistentAttributeType.EMBEDDED;
	}

	@Override
	public PersistenceType getPersistenceType() {
		return PersistenceType.EMBEDDABLE;
	}

	@Override
	public SingularAttributeClassification getAttributeTypeClassification() {
		return SingularAttributeClassification.EMBEDDED;
	}

	@Override
	public String asLoggableText() {
		return "IdentifierCompositeAggregated(" + embeddedPersister.asLoggableText() + ")";
	}

	@Override
	public QueryResult generateQueryResult(
			NavigableReference selectedExpression,
			String resultVariable,
			ColumnReferenceSource columnReferenceSource,
			SqlSelectionResolver sqlSelectionResolver,
			QueryResultCreationContext creationContext) {
		return embeddedPersister.generateQueryResult(
				selectedExpression,
				resultVariable,
				columnReferenceSource,
				sqlSelectionResolver,
				creationContext
		);
	}
}

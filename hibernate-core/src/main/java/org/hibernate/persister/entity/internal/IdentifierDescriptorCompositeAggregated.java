/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.entity.internal;

import java.util.List;

import org.hibernate.mapping.Property;
import org.hibernate.persister.common.NavigableRole;
import org.hibernate.persister.common.internal.PersisterHelper;
import org.hibernate.persister.common.spi.AbstractSingularPersistentAttribute;
import org.hibernate.persister.common.spi.Column;
import org.hibernate.persister.common.spi.JoinColumnMapping;
import org.hibernate.persister.common.spi.Navigable;
import org.hibernate.persister.common.spi.NavigableContainer;
import org.hibernate.persister.common.spi.NavigableVisitationStrategy;
import org.hibernate.persister.common.spi.PersistentAttribute;
import org.hibernate.persister.common.spi.SingularPersistentAttribute;
import org.hibernate.persister.embedded.spi.EmbeddedPersister;
import org.hibernate.persister.entity.spi.EntityHierarchy;
import org.hibernate.persister.entity.spi.IdentifierDescriptor;
import org.hibernate.persister.spi.PersisterCreationContext;
import org.hibernate.sql.ast.produce.result.spi.QueryResult;
import org.hibernate.sql.ast.produce.result.spi.QueryResultCreationContext;
import org.hibernate.sql.ast.produce.result.spi.SqlSelectionResolver;
import org.hibernate.sql.ast.tree.internal.NavigableSelection;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.ast.tree.spi.expression.domain.ColumnReferenceSource;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;
import org.hibernate.sql.ast.tree.spi.select.Selection;
import org.hibernate.type.descriptor.java.spi.EmbeddableJavaDescriptor;
import org.hibernate.type.spi.EmbeddedType;

/**
 * @author Steve Ebersole
 */
public class IdentifierDescriptorCompositeAggregated<O,J>
		extends AbstractSingularPersistentAttribute<O,J,EmbeddedType<J>>
		implements IdentifierDescriptor<O,J>, SingularPersistentAttribute<O,J>, NavigableContainer<J> {
	private final EntityHierarchy entityHierarchy;
	private final EmbeddedPersister<J> embeddedPersister;

	@SuppressWarnings("unchecked")
	public IdentifierDescriptorCompositeAggregated(
			EntityHierarchy entityHierarchy,
			Property idAttribute,
			EmbeddedPersister<J> embeddedPersister,
			PersisterCreationContext creationContext) {
		super(
				entityHierarchy.getRootEntityPersister(),
				idAttribute.getName(),
				PersisterHelper.resolvePropertyAccess( entityHierarchy.getRootEntityPersister(), idAttribute, creationContext ),
				embeddedPersister.getOrmType(),
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
	public List<JoinColumnMapping> resolveJoinColumnMappings(PersistentAttribute persistentAttribute) {
		return getContainer().resolveJoinColumnMappings( persistentAttribute );
	}

	@Override
	public Selection createSelection(Expression selectedExpression, String resultVariable) {
		assert selectedExpression instanceof NavigableReference;
		return new NavigableSelection( (NavigableReference) selectedExpression, resultVariable );
	}

	@Override
	public QueryResult generateReturn(
			NavigableReference selectedExpression,
			String resultVariable,
			ColumnReferenceSource columnReferenceSource,
			SqlSelectionResolver sqlSelectionResolver,
			QueryResultCreationContext creationContext) {
		return embeddedPersister.generateReturn(
				selectedExpression,
				resultVariable,
				columnReferenceSource,
				sqlSelectionResolver,
				creationContext
		);
	}
}

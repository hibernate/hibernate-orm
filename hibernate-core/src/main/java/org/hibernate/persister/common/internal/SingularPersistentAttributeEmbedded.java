/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.persister.common.internal;

import java.util.List;

import org.hibernate.persister.common.NavigableRole;
import org.hibernate.persister.common.spi.AbstractSingularPersistentAttribute;
import org.hibernate.persister.model.relational.spi.Column;
import org.hibernate.persister.common.spi.ManagedTypeImplementor;
import org.hibernate.persister.common.spi.Navigable;
import org.hibernate.persister.common.spi.NavigableVisitationStrategy;
import org.hibernate.persister.common.spi.SingularPersistentAttribute;
import org.hibernate.persister.embedded.spi.EmbeddedPersister;
import org.hibernate.persister.embedded.spi.EmbeddedValuedNavigable;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.sql.ast.produce.result.internal.QueryResultCompositeImpl;
import org.hibernate.sql.ast.produce.result.spi.QueryResult;
import org.hibernate.sql.ast.produce.result.spi.QueryResultCreationContext;
import org.hibernate.sql.ast.produce.result.spi.SqlSelectionResolver;
import org.hibernate.sql.ast.tree.internal.NavigableSelection;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.ast.tree.spi.expression.domain.ColumnReferenceSource;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;
import org.hibernate.sql.ast.tree.spi.select.Selection;
import org.hibernate.type.descriptor.java.spi.EmbeddableJavaDescriptor;

/**
 * @author Steve Ebersole
 */
public class SingularPersistentAttributeEmbedded<O,J>
		extends AbstractSingularPersistentAttribute<O,J>
		implements SingularPersistentAttribute<O,J>, EmbeddedValuedNavigable<J> {

	private final EmbeddedPersister<J> embeddedPersister;

	public SingularPersistentAttributeEmbedded(
			ManagedTypeImplementor<O> declaringType,
			String attributeName,
			PropertyAccess propertyAccess,
			Disposition disposition,
			EmbeddedPersister<J> embeddedPersister) {
		super( declaringType, attributeName, propertyAccess, embeddedPersister, disposition, true );
		this.embeddedPersister = embeddedPersister;
	}

	@Override
	public ManagedTypeImplementor<O> getContainer() {
		return super.getContainer();
	}

	@Override
	public EmbeddedPersister<J> getEmbeddedPersister() {
		return embeddedPersister;
	}

	@Override
	public EmbeddableJavaDescriptor<J> getJavaTypeDescriptor() {
		return (EmbeddableJavaDescriptor<J>) super.getJavaTypeDescriptor();
	}

	@Override
	public SingularAttributeClassification getAttributeTypeClassification() {
		return SingularAttributeClassification.EMBEDDED;
	}

	@Override
	public List<Column> getColumns() {
		return embeddedPersister.collectColumns();
	}

	@Override
	public String asLoggableText() {
		return toString();
	}

	@Override
	public PersistentAttributeType getPersistentAttributeType() {
		return PersistentAttributeType.EMBEDDED;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <N> Navigable<N> findNavigable(String navigableName) {
		return getEmbeddedPersister().findNavigable( navigableName );
	}

	@Override
	public NavigableRole getNavigableRole() {
		return embeddedPersister.getNavigableRole();
	}

	@Override
	public void visitNavigable(NavigableVisitationStrategy visitor) {
		visitor.visitSingularAttributeEmbedded( this );
	}

	@Override
	public <N> Navigable<N> findDeclaredNavigable(String navigableName) {
		return embeddedPersister.findDeclaredNavigable( navigableName );
	}

	@Override
	public List<Navigable> getNavigables() {
		return embeddedPersister.getNavigables();
	}

	@Override
	public List<Navigable> getDeclaredNavigables() {
		return embeddedPersister.getDeclaredNavigables();
	}

	@Override
	public void visitNavigables(NavigableVisitationStrategy visitor) {
		embeddedPersister.visitNavigables( visitor );
	}

	@Override
	public void visitDeclaredNavigables(NavigableVisitationStrategy visitor) {
		embeddedPersister.visitDeclaredNavigables( visitor );
	}

	@Override
	public Selection createSelection(Expression selectedExpression, String resultVariable) {
		assert selectedExpression instanceof NavigableReference;
		return new NavigableSelection( (NavigableReference) selectedExpression, resultVariable );
	}

	@Override
	public QueryResult generateQueryResult(
			NavigableReference selectedExpression,
			String resultVariable,
			ColumnReferenceSource columnReferenceSource,
			SqlSelectionResolver sqlSelectionResolver,
			QueryResultCreationContext creationContext) {
		return new QueryResultCompositeImpl( selectedExpression, resultVariable, embeddedPersister );
	}
}

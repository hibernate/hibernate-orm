/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import java.util.List;

import org.hibernate.id.IdentifierGenerator;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.model.domain.spi.AbstractSingularPersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.EmbeddedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityHierarchy;
import org.hibernate.metamodel.model.domain.spi.EntityIdentifierCompositeAggregated;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.metamodel.model.domain.spi.NavigableVisitationStrategy;
import org.hibernate.metamodel.model.domain.spi.SingularPersistentAttribute;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.sql.NotYetImplementedException;
import org.hibernate.sql.ast.produce.spi.SqlExpressionQualifier;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;
import org.hibernate.sql.results.spi.QueryResult;
import org.hibernate.sql.results.spi.QueryResultCreationContext;
import org.hibernate.sql.results.spi.SqlSelectionGroup;
import org.hibernate.sql.results.spi.SqlSelectionGroupResolutionContext;
import org.hibernate.type.descriptor.java.spi.EmbeddableJavaDescriptor;

import static org.hibernate.metamodel.model.domain.internal.PersisterHelper.resolvePropertyAccess;

/**
 * @author Steve Ebersole
 */
public class EntityIdentifierCompositeAggregatedImpl<O,J>
		extends AbstractSingularPersistentAttribute<O,J>
		implements EntityIdentifierCompositeAggregated<O,J> {
	private final EmbeddedTypeDescriptor<J> embeddedMetadata;

	@SuppressWarnings("unchecked")
	public EntityIdentifierCompositeAggregatedImpl(
			EntityHierarchy entityHierarchy,
			Property idAttribute,
			EmbeddedTypeDescriptor<J> embeddedMetadata,
			RuntimeModelCreationContext creationContext) {
		super(
				entityHierarchy.getRootEntityType(),
				idAttribute.getName(),
				resolvePropertyAccess( entityHierarchy.getRootEntityType(), idAttribute, creationContext ),
				Disposition.ID,
				false,
				idAttribute.getValue()
		);
		this.embeddedMetadata = embeddedMetadata;
	}

	@Override
	public EmbeddedTypeDescriptor getEmbeddedDescriptor() {
		return embeddedMetadata;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// NavigableSource (embedded)

	@Override
	public NavigableRole getNavigableRole() {
		return embeddedMetadata.getNavigableRole();
	}

	@Override
	public EmbeddableJavaDescriptor<J> getJavaTypeDescriptor() {
		return embeddedMetadata.getJavaTypeDescriptor();
	}

	@Override
	public int getNumberOfJdbcParametersForRestriction() {
		return embeddedMetadata.getNumberOfJdbcParametersForRestriction();
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
		return "IdentifierCompositeAggregated(" + embeddedMetadata.asLoggableText() + ")";
	}

	@Override
	public void visitNavigable(NavigableVisitationStrategy visitor) {

	}

	@Override
	public QueryResult createQueryResult(
			NavigableReference expression,
			String resultVariable,
			QueryResultCreationContext creationContext) {
		return embeddedMetadata.createQueryResult(
				expression,
				resultVariable,
				creationContext
		);
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
	public IdentifierGenerator getIdentifierValueGenerator() {
		throw new NotYetImplementedException(  );
	}

	@Override
	public List<Column> getColumns() {
		throw new NotYetImplementedException(  );
	}

	@Override
	public SqlSelectionGroup resolveSqlSelectionGroup(
			SqlExpressionQualifier qualifier,
			SqlSelectionGroupResolutionContext resolutionContext) {
		throw new NotYetImplementedException(  );
	}

	@Override
	public <N> Navigable<N> findNavigable(String navigableName) {
		throw new NotYetImplementedException(  );
	}

	@Override
	public <N> Navigable<N> findDeclaredNavigable(String navigableName) {
		throw new NotYetImplementedException(  );
	}

	@Override
	public List<Navigable> getNavigables() {
		throw new NotYetImplementedException(  );
	}

	@Override
	public List<Navigable> getDeclaredNavigables() {
		throw new NotYetImplementedException(  );
	}

	@Override
	public void visitNavigables(NavigableVisitationStrategy visitor) {
		throw new NotYetImplementedException(  );
	}

	@Override
	public void visitDeclaredNavigables(NavigableVisitationStrategy visitor) {
		throw new NotYetImplementedException(  );
	}
}

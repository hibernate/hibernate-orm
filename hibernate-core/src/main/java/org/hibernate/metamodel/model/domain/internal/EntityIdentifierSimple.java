/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import java.util.List;

import org.hibernate.mapping.Property;
import org.hibernate.metamodel.model.domain.spi.AbstractSingularPersistentAttribute;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.metamodel.model.domain.spi.NavigableVisitationStrategy;
import org.hibernate.metamodel.model.domain.spi.SingularPersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.EntityHierarchy;
import org.hibernate.metamodel.model.domain.spi.IdentifiableTypeImplementor;
import org.hibernate.metamodel.model.domain.spi.EntityIdentifier;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.sql.ast.produce.result.spi.QueryResult;
import org.hibernate.sql.ast.produce.result.spi.QueryResultCreationContext;
import org.hibernate.sql.ast.produce.result.spi.SqlSelectionResolver;
import org.hibernate.sql.ast.tree.spi.expression.domain.ColumnReferenceSource;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;
import org.hibernate.type.spi.BasicType;

/**
 * @author Steve Ebersole
 */
public class EntityIdentifierSimple<O,J>
		extends AbstractSingularPersistentAttribute<O,J>
		implements EntityIdentifier<O,J>, SingularPersistentAttribute<O,J> {
	private final EntityHierarchy hierarchy;
	private final List<Column> columns;

	public EntityIdentifierSimple(
			EntityHierarchy hierarchy,
			IdentifiableTypeImplementor declarer,
			Property property,
			BasicType<J> ormType,
			List<Column> columns,
			RuntimeModelCreationContext creationContext) {
		super(
				hierarchy.getRootEntityType(),
				property.getName(),
				PersisterHelper.resolvePropertyAccess( declarer, property, creationContext ),
				ormType,
				Disposition.ID,
				false
		);
		this.hierarchy = hierarchy;
		this.columns = columns;
	}

	@Override
	public boolean hasSingleIdAttribute() {
		return true;
	}

	@Override
	public List<Column> getColumns() {
		return columns;
	}

	@Override
	public SingularPersistentAttribute<O,J> getIdAttribute() {
		return this;
	}

	@Override
	public SingularAttributeClassification getAttributeTypeClassification() {
		return SingularAttributeClassification.BASIC;
	}

	@Override
	public String asLoggableText() {
		return "IdentifierSimple(" + getContainer().asLoggableText() + ")";
	}

	@Override
	public PersistentAttributeType getPersistentAttributeType() {
		return PersistentAttributeType.BASIC;
	}

	@Override
	public PersistenceType getPersistenceType() {
		return PersistenceType.BASIC;
	}

	@Override
	public void visitNavigable(NavigableVisitationStrategy visitor) {
		visitor.visitSimpleIdentifier( hierarchy, getIdAttribute() );
	}

	@Override
	public QueryResult generateQueryResult(
			NavigableReference selectedExpression,
			String resultVariable,
			ColumnReferenceSource columnReferenceSource,
			SqlSelectionResolver sqlSelectionResolver,
			QueryResultCreationContext creationContext) {
		return getIdAttribute().generateQueryResult( selectedExpression, resultVariable, columnReferenceSource, sqlSelectionResolver, creationContext );
	}
}

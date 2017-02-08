/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.entity.internal;

import java.util.List;

import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.mapping.Property;
import org.hibernate.persister.common.internal.PersisterHelper;
import org.hibernate.persister.common.spi.AbstractSingularPersistentAttribute;
import org.hibernate.persister.common.spi.Column;
import org.hibernate.persister.common.spi.JoinColumnMapping;
import org.hibernate.persister.common.spi.Navigable;
import org.hibernate.persister.common.spi.NavigableSource;
import org.hibernate.persister.common.spi.NavigableVisitationStrategy;
import org.hibernate.persister.common.spi.PersistentAttribute;
import org.hibernate.persister.common.spi.SingularPersistentAttribute;
import org.hibernate.persister.embedded.spi.EmbeddedPersister;
import org.hibernate.persister.entity.spi.EntityHierarchy;
import org.hibernate.persister.entity.spi.IdentifierDescriptor;
import org.hibernate.sql.ast.expression.Expression;
import org.hibernate.sql.ast.expression.domain.NavigablePath;
import org.hibernate.sql.ast.expression.domain.NavigableReferenceExpression;
import org.hibernate.sql.ast.expression.domain.SingularAttributeReferenceExpression;
import org.hibernate.sql.ast.from.ColumnBinding;
import org.hibernate.sql.ast.from.TableGroup;
import org.hibernate.sql.ast.from.TableSpace;
import org.hibernate.sql.ast.select.Selectable;
import org.hibernate.sql.ast.select.SelectableEmbeddedTypeImpl;
import org.hibernate.sql.convert.internal.FromClauseIndex;
import org.hibernate.sql.convert.internal.SqlAliasBaseManager;
import org.hibernate.sql.convert.results.spi.Fetch;
import org.hibernate.sql.convert.results.spi.FetchParent;
import org.hibernate.sql.convert.results.spi.Return;
import org.hibernate.sql.convert.results.spi.ReturnResolutionContext;
import org.hibernate.sql.exec.spi.SqlSelectAstToJdbcSelectConverter;
import org.hibernate.type.spi.EmbeddedType;
import org.hibernate.type.spi.Type;

/**
 * @author Steve Ebersole
 */
public class IdentifierDescriptorCompositeAggregated<O,J>
		extends AbstractSingularPersistentAttribute<O,J,EmbeddedType<J>>
		implements IdentifierDescriptor<O,J>, SingularPersistentAttribute<O,J>, NavigableSource<J> {
	private final EntityHierarchy entityHierarchy;
	private final EmbeddedPersister<J> embeddedPersister;

	@SuppressWarnings("unchecked")
	public IdentifierDescriptorCompositeAggregated(
			EntityHierarchy entityHierarchy,
			Property idAttribute,
			EmbeddedPersister<J> embeddedPersister) {
		super(
				entityHierarchy.getRootEntityPersister(),
				idAttribute.getName(),
				PersisterHelper.resolvePropertyAccess( entityHierarchy.getRootEntityPersister(), idAttribute ),
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
	public Navigable findNavigable(String navigableName) {
		return embeddedPersister.findNavigable( navigableName );
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

	@Override
	public TableGroup buildTableGroup(
			TableSpace tableSpace,
			SqlAliasBaseManager sqlAliasBaseManager,
			FromClauseIndex fromClauseIndex) {
		throw new NotYetImplementedException(  );
	}

	@Override
	public Return generateReturn(ReturnResolutionContext returnResolutionContext, TableGroup tableGroup) {
		// todo : not sure what we will need here yet...

		// for now...
		return new SelectableImpl( this, returnResolutionContext, tableGroup ).toQueryReturn( returnResolutionContext, null );
	}

	@Override
	public Fetch generateFetch(ReturnResolutionContext returnResolutionContext, TableGroup tableGroup, FetchParent fetchParent) {
		throw new NotYetImplementedException(  );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// IdentifierDescriptor

	@Override
	public EmbeddedType getIdType() {
		return getOrmType();
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
		return getSource().resolveJoinColumnMappings( persistentAttribute );
	}


	private static class SelectableImpl implements Selectable, NavigableReferenceExpression {
		private final SingularAttributeReferenceExpression expressionDelegate;
		private final SelectableEmbeddedTypeImpl selectableDelegate;
		private final NavigablePath navigablePath;

		public SelectableImpl(
				IdentifierDescriptorCompositeAggregated idDescriptor,
				ReturnResolutionContext returnResolutionContext,
				TableGroup tableGroup) {
			this.navigablePath = returnResolutionContext.currentNavigablePath().append( idDescriptor.getNavigableName() );
			this.expressionDelegate = new SingularAttributeReferenceExpression(
					tableGroup,
					idDescriptor,
					navigablePath
			);
			this.selectableDelegate = new SelectableEmbeddedTypeImpl(
					this,
					getColumnBindings(),
					(EmbeddedType) expressionDelegate.getType()
			);
		}

		@Override
		public Type getType() {
			return expressionDelegate.getType();
		}

		@Override
		public Selectable getSelectable() {
			return this;
		}

		@Override
		public void accept(SqlSelectAstToJdbcSelectConverter walker) {
			// todo (6.0) : do we need a separate "visitEntityIdentifier" method(s)?

			walker.visitSingularAttributeReference( expressionDelegate );
		}

		@Override
		public Expression getSelectedExpression() {
			return expressionDelegate;
		}

		@Override
		public Return toQueryReturn(ReturnResolutionContext returnResolutionContext, String resultVariable) {
			return selectableDelegate.toQueryReturn( returnResolutionContext, resultVariable );
		}

		@Override
		public Navigable getNavigable() {
			return expressionDelegate.getNavigable();
		}

		@Override
		public NavigablePath getNavigablePath() {
			return navigablePath;
		}

		@Override
		public List<ColumnBinding> getColumnBindings() {
			return expressionDelegate.getColumnBindings();
		}
	}
}

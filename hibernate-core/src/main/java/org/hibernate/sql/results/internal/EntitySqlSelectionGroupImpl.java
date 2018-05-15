/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.annotations.Remove;
import org.hibernate.metamodel.model.domain.spi.DiscriminatorDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityHierarchy;
import org.hibernate.metamodel.model.domain.spi.EntityIdentifier;
import org.hibernate.metamodel.model.domain.spi.RowIdDescriptor;
import org.hibernate.metamodel.model.domain.spi.StateArrayContributor;
import org.hibernate.metamodel.model.domain.spi.StateArrayContributorContainer;
import org.hibernate.metamodel.model.domain.spi.TenantDiscrimination;
import org.hibernate.sql.ast.produce.spi.ColumnReferenceQualifier;
import org.hibernate.sql.ast.produce.spi.SqlAstCreationContext;
import org.hibernate.sql.results.spi.EntitySqlSelectionGroup;
import org.hibernate.sql.results.spi.AssemblerCreationContext;
import org.hibernate.sql.results.spi.SqlSelection;
import org.hibernate.sql.results.spi.SqlSelectionGroupNode;

/**
 * @author Steve Ebersole
 */
@Remove
public class EntitySqlSelectionGroupImpl extends AbstractSqlSelectionGroup implements EntitySqlSelectionGroup {

	public static EntitySqlSelectionGroup buildSqlSelectionGroup(
			EntityTypeDescriptor<?> entityDescriptor,
			ColumnReferenceQualifier qualifier,
			SqlAstCreationContext resolutionContext) {
		return new Builder( entityDescriptor ).create( qualifier, resolutionContext );
	}

	private final EntityTypeDescriptor<?> entityDescriptor;

	private final SqlSelectionGroupNode idSqlSelectionGroup;
	private final SqlSelectionGroupNode discriminatorSqlSelection;
	private final SqlSelectionGroupNode tenantDiscriminatorSqlSelection;
	private final SqlSelectionGroupNode rowIdSqlSelection;

	private EntitySqlSelectionGroupImpl(
			EntityTypeDescriptor<?> entityDescriptor,
			SqlSelectionGroupNode idSqlSelectionGroup,
			SqlSelectionGroupNode discriminatorSqlSelection,
			SqlSelectionGroupNode tenantDiscriminatorSqlSelection,
			SqlSelectionGroupNode rowIdSqlSelection,
			Map<StateArrayContributor<?>, SqlSelectionGroupNode> sqlSelectionsByContributor) {
		super( sqlSelectionsByContributor );
		this.entityDescriptor = entityDescriptor;
		this.rowIdSqlSelection = rowIdSqlSelection;
		this.idSqlSelectionGroup = idSqlSelectionGroup;
		this.discriminatorSqlSelection = discriminatorSqlSelection;
		this.tenantDiscriminatorSqlSelection = tenantDiscriminatorSqlSelection;
	}

	@Override
	protected StateArrayContributorContainer getContributorContainer() {
		return entityDescriptor;
	}

	@Override
	public SqlSelectionGroupNode getIdSqlSelections() {
		return idSqlSelectionGroup;
	}

	@Override
	public SqlSelectionGroupNode getDiscriminatorSqlSelection() {
		return discriminatorSqlSelection;
	}

	@Override
	public SqlSelectionGroupNode getTenantDiscriminatorSqlSelection() {
		return tenantDiscriminatorSqlSelection;
	}

	@Override
	public SqlSelectionGroupNode getRowIdSqlSelection() {
		return rowIdSqlSelection;
	}

	@SuppressWarnings({"UnusedReturnValue", "unchecked", "WeakerAccess"})
	public static class Builder {
		private final EntityTypeDescriptor<?> entityDescriptor;

		private SqlSelectionGroupNode idSqlSelections;
		private SqlSelectionGroupNode discriminatorSqlSelection;
		private SqlSelectionGroupNode tenantDiscriminatorSqlSelection;
		private SqlSelectionGroupNode rowIdSqlSelection;
		private Map<StateArrayContributor<?>, SqlSelectionGroupNode> sqlSelectionsByContributor;

		public Builder(EntityTypeDescriptor<?> entityDescriptor) {
			this.entityDescriptor = entityDescriptor;
		}

		public EntityTypeDescriptor<?> getEntityDescriptor() {
			return entityDescriptor;
		}


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Identifier

		protected void applyIdSqlSelections(
				EntityIdentifier identifierDescriptor,
				ColumnReferenceQualifier qualifier,
				AssemblerCreationContext creationContext) {
//			applyIdSqlSelections( identifierDescriptor.resolveSqlSelections( qualifier, creationContext ) );
		}

		protected final void applyIdSqlSelections(SqlSelectionGroupNode idSqlSelectionGroup) {
			if ( this.idSqlSelections != null ) {
				throw new HibernateException( "Multiple calls to set entity id SqlSelections" );
			}
			this.idSqlSelections = idSqlSelectionGroup;
		}


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Discriminator

		protected void applyDiscriminatorSqlSelection(
				DiscriminatorDescriptor discriminatorDescriptor,
				ColumnReferenceQualifier qualifier,
				AssemblerCreationContext creationContext) {
//			applyDiscriminatorSqlSelection( discriminatorDescriptor.resolveSqlSelection( qualifier, creationContext ) );
		}

		protected final void applyDiscriminatorSqlSelection(SqlSelection discriminatorSqlSelection) {
			if ( this.discriminatorSqlSelection != null ) {
				throw new HibernateException( "Multiple calls to set entity discriminator SqlSelection" );
			}
			this.discriminatorSqlSelection = discriminatorSqlSelection;
		}


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Tenant-discriminator

		protected void applyTenantDiscriminatorSqlSelection(
				TenantDiscrimination tenantDiscrimination,
				ColumnReferenceQualifier qualifier,
				AssemblerCreationContext creationContext) {
//			applyTenantDiscriminatorSqlSelection(
//					creationContext.getSqlSelectionResolver().resolveSqlSelection(
//							creationContext.getSqlSelectionResolver().resolveSqlExpression(
//									qualifier,
//									tenantDiscrimination.getBoundColumn()
//							),
//							tenantDiscrimination.getJavaTypeDescriptor(),
//							creationContext.getSessionFactory().getTypeConfiguration()
//					)
//			);
		}

		protected final void applyTenantDiscriminatorSqlSelection(SqlSelection tenantDiscriminatorSqlSelection) {
			if ( this.tenantDiscriminatorSqlSelection != null ) {
				throw new HibernateException( "Multiple calls to set entity tenant-discriminator SqlSelection" );
			}
			this.tenantDiscriminatorSqlSelection = tenantDiscriminatorSqlSelection;
		}


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Row-id

		protected void applyRowIdSqlSelection(
				RowIdDescriptor rowIdDescriptor,
				ColumnReferenceQualifier qualifier,
				AssemblerCreationContext creationContext) {
//			applyRowIdSqlSelection(
//					creationContext.getSqlSelectionResolver().resolveSqlSelection(
//							creationContext.getSqlSelectionResolver().resolveSqlExpression(
//									qualifier,
//									rowIdDescriptor.getBoundColumn()
//							),
//							rowIdDescriptor.getJavaTypeDescriptor(),
//							creationContext.getSessionFactory().getTypeConfiguration()
//					)
//			);
		}

		protected final void applyRowIdSqlSelection(SqlSelection rowIdSqlSelection) {
			this.rowIdSqlSelection = rowIdSqlSelection;
		}


		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// State-array-contributors

		protected void applyContributorSqlSelections(
				StateArrayContributor<?> contributor,
				ColumnReferenceQualifier qualifier,
				AssemblerCreationContext creationContext) {
//			applyContributorSqlSelections(
//					contributor,
//					contributor.resolveSqlSelections( qualifier, creationContext )
//			);
		}

		protected final void applyContributorSqlSelections(StateArrayContributor<?> contributor, SqlSelectionGroupNode sqlSelections) {
			if ( sqlSelectionsByContributor == null ) {
				sqlSelectionsByContributor = new HashMap<>();
			}
			sqlSelectionsByContributor.put( contributor, sqlSelections );
		}

		public EntitySqlSelectionGroupImpl create(ColumnReferenceQualifier qualifier, AssemblerCreationContext resolutionContext) {
			// todo (6.0) : should return something like `EntityStateAssemblers`
			// 		which defines the various DomainResultAssemblers for its state (id, state-array-contributor, etc).
			//
			// 		Another option would be to

			// todo (6.0) : need the "fetch graph" / "entity graph" to really be able to perform this correctly
			//		the plan is that information would be available from SqlAstCreationContext -
			//		something like `SqlAstCreationContext#getCurrentAttr
			final EntityHierarchy hierarchy = entityDescriptor.getHierarchy();

			applyIdSqlSelections(
					hierarchy.getIdentifierDescriptor(),
					qualifier,
					resolutionContext
			);

			if ( hierarchy.getDiscriminatorDescriptor() != null ) {
				applyDiscriminatorSqlSelection(
						hierarchy.getDiscriminatorDescriptor(),
						qualifier,
						resolutionContext
				);
			}

			if ( hierarchy.getTenantDiscrimination() != null ) {
				applyTenantDiscriminatorSqlSelection(
						hierarchy.getTenantDiscrimination(),
						qualifier,
						resolutionContext
				);
			}

			if ( hierarchy.getRowIdDescriptor() != null ) {
				applyRowIdSqlSelection(
						hierarchy.getRowIdDescriptor(),
						qualifier,
						resolutionContext
				);
			}

			for ( StateArrayContributor<?> contributor : entityDescriptor.getStateArrayContributors() ) {
				applyContributorSqlSelections(
						contributor,
						qualifier,
						resolutionContext
				);
			}

			return new EntitySqlSelectionGroupImpl(
					entityDescriptor,
					idSqlSelections,
					discriminatorSqlSelection,
					tenantDiscriminatorSqlSelection,
					rowIdSqlSelection,
					sqlSelectionsByContributor
			);
		}
	}
}

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal;

import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.metamodel.model.domain.internal.BasicSingularPersistentAttribute;
import org.hibernate.metamodel.model.domain.internal.SingularPersistentAttributeEmbedded;
import org.hibernate.metamodel.model.domain.internal.SingularPersistentAttributeEntity;
import org.hibernate.metamodel.model.domain.spi.DiscriminatorDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityIdentifierCompositeAggregated;
import org.hibernate.metamodel.model.domain.spi.EntityIdentifierCompositeNonAggregated;
import org.hibernate.metamodel.model.domain.spi.EntityIdentifierSimple;
import org.hibernate.metamodel.model.domain.spi.NavigableVisitationStrategy;
import org.hibernate.metamodel.model.domain.spi.PluralPersistentAttribute;
import org.hibernate.sql.ast.produce.internal.CompositeColumnReferenceQualifier;
import org.hibernate.sql.ast.produce.spi.ColumnReferenceQualifier;
import org.hibernate.sql.results.spi.EntitySqlSelectionMappings;
import org.hibernate.sql.results.spi.QueryResultCreationContext;
import org.hibernate.sql.results.spi.SqlSelection;
import org.hibernate.sql.results.spi.SqlSelectionGroup;

/**
 * @author Steve Ebersole
 */
public class EntitySqlSelectionMappingsBuilder implements NavigableVisitationStrategy {
	public static EntitySqlSelectionMappings buildSqlSelectionMappings(
			EntityDescriptor entityDescriptor,
			ColumnReferenceQualifier qualifier,
			QueryResultCreationContext creationContext) {
		final EntitySqlSelectionMappingsBuilder strategy = new EntitySqlSelectionMappingsBuilder(
				qualifier,
				creationContext
		);
		entityDescriptor.visitNavigables( strategy );
		return strategy.buildSqlSelectionMappings();
	}

	private final ColumnReferenceQualifier qualifier;
	private final QueryResultCreationContext creationContext;
	private final EntitySqlSelectionMappingsImpl.Builder sqlSelectionMappingsBuilder = new EntitySqlSelectionMappingsImpl.Builder();

	protected EntitySqlSelectionMappingsBuilder(
			ColumnReferenceQualifier qualifier,
			QueryResultCreationContext creationContext) {
		this.qualifier = qualifier;
		this.creationContext = creationContext;
	}

	protected QueryResultCreationContext getCreationContext() {
		return creationContext;
	}

	protected EntitySqlSelectionMappings buildSqlSelectionMappings() {
		return sqlSelectionMappingsBuilder.create();
	}

	protected ColumnReferenceQualifier getQualifier() {
		return qualifier;
	}

	@Override
	public void visitSimpleIdentifier(EntityIdentifierSimple identifier) {
		setIdSqlSelectionGroup(
				identifier.resolveSqlSelectionGroup(
						qualifier,
						getCreationContext()
				)
		);
	}

	protected void setIdSqlSelectionGroup(List<SqlSelection> group) {
		sqlSelectionMappingsBuilder.applyIdSqlSelectionGroup( group );
	}

	@Override
	public void visitAggregateCompositeIdentifier(EntityIdentifierCompositeAggregated identifier) {
		setIdSqlSelectionGroup(
				identifier.resolveSqlSelectionGroup( qualifier, getCreationContext() )
		);
	}

	@Override
	public void visitNonAggregateCompositeIdentifier(EntityIdentifierCompositeNonAggregated identifier) {
		setIdSqlSelectionGroup(
				identifier.resolveSqlSelectionGroup( qualifier, getCreationContext() )
		);
	}

	@Override
	public void visitDiscriminator(DiscriminatorDescriptor discriminator) {
		setDiscriminatorSqlSelection(
				discriminator.resolveSqlSelection( qualifier, getCreationContext() )
		);
	}

	protected void setDiscriminatorSqlSelection(SqlSelectionGroup sqlSelectionGroup) {
		if ( sqlSelectionGroup != null ) {
			if ( sqlSelectionGroup.getSqlSelections().size() > 1 ) {
				throw new HibernateException( "Attempting to set multiple SqlSelections for discriminator" );
			}
		}

		setDiscriminatorSqlSelection( sqlSelectionGroup.getSqlSelections().get( 0 ) );
	}

	protected void setDiscriminatorSqlSelection(SqlSelection discriminatorSqlSelection) {
		sqlSelectionMappingsBuilder.applyDiscriminatorSqlSelection( discriminatorSqlSelection );
	}

	@Override
	public void visitSingularAttributeBasic(BasicSingularPersistentAttribute attribute) {
		sqlSelectionMappingsBuilder.applyAttributeSqlSelectionGroup(
				attribute,
				attribute.resolveSqlSelectionGroup( qualifier, getCreationContext() )
		);
	}

	@Override
	public void visitSingularAttributeEmbedded(SingularPersistentAttributeEmbedded attribute) {
		sqlSelectionMappingsBuilder.applyAttributeSqlSelectionGroup(
				attribute,
				attribute.resolveSqlSelectionGroup( qualifier, creationContext )
		);
	}

	@Override
	public void visitSingularAttributeEntity(SingularPersistentAttributeEntity attribute) {
		sqlSelectionMappingsBuilder.applyAttributeSqlSelectionGroup(
				attribute,
				attribute.resolveSqlSelectionGroup( qualifier, creationContext )
		);
	}

	@Override
	public void visitPluralAttribute(PluralPersistentAttribute attribute) {
		// determine the table that is the source of the FK - this will either
		//		be the "collection table" or the "element table"
		final CompositeColumnReferenceQualifier extendedQulifier;

		// todo (6.0) : we need some way here to resolve or create a TableReference
		//		we may also need the ability to create new TableGroups and TableReferences
		//		on the fly.
		//
		// we need this here because we need to build an expanded "qualifier" to
		//		cover the "collection table"
		//
		// Might be a good place to hook in fetch processing as well, provided we
		//		solve the need to create TableGroups on the fly

		sqlSelectionMappingsBuilder.applyAttributeSqlSelectionGroup(
				attribute,
				attribute.resolveSqlSelectionGroup( qualifier, creationContext )
		);
	}
}

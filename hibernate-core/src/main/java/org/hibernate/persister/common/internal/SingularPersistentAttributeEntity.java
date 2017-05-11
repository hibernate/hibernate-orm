/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.persister.common.internal;

import java.util.List;

import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.persister.common.NavigableRole;
import org.hibernate.persister.common.spi.AbstractSingularPersistentAttribute;
import org.hibernate.persister.common.spi.Column;
import org.hibernate.persister.common.spi.JoinColumnMapping;
import org.hibernate.persister.common.spi.JoinablePersistentAttribute;
import org.hibernate.persister.common.spi.ManagedTypeImplementor;
import org.hibernate.persister.common.spi.Navigable;
import org.hibernate.persister.common.spi.NavigableVisitationStrategy;
import org.hibernate.persister.common.spi.PersistentAttribute;
import org.hibernate.persister.entity.spi.EntityPersister;
import org.hibernate.persister.entity.spi.EntityValuedNavigable;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.sql.ast.tree.spi.from.TableGroup;
import org.hibernate.sql.ast.tree.spi.from.TableSpace;
import org.hibernate.sql.ast.produce.spi.FromClauseIndex;
import org.hibernate.sql.ast.produce.spi.SqlAliasBaseManager;
import org.hibernate.sql.ast.produce.result.spi.Fetch;
import org.hibernate.sql.ast.produce.result.spi.FetchParent;
import org.hibernate.sql.ast.produce.result.spi.QueryResult;
import org.hibernate.sql.ast.produce.result.spi.QueryResultCreationContext;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.spi.EntityType;


/**
 * @author Steve Ebersole
 */
public class SingularPersistentAttributeEntity<O,J>
		extends AbstractSingularPersistentAttribute<O,J,EntityType<J>>
		implements JoinablePersistentAttribute<O,J>, EntityValuedNavigable<J> {
	private final SingularAttributeClassification classification;
	private final EntityPersister entityPersister;
	private final List<Column> columns;
	private final NavigableRole navigableRole;

	private List<JoinColumnMapping> joinColumnMappings;

	public SingularPersistentAttributeEntity(
			ManagedTypeImplementor declaringType,
			String name,
			PropertyAccess propertyAccess,
			SingularAttributeClassification classification,
			EntityType ormType,
			Disposition disposition,
			EntityPersister entityPersister,
			List<Column> columns) {
		super( declaringType, name, propertyAccess, ormType, disposition, true );
		this.classification = classification;
		this.entityPersister = entityPersister;
		this.navigableRole = declaringType.getNavigableRole().append( name );

		// columns should be the rhs columns I believe.
		//		todo : add an assertion based on whatever this should be...
		this.columns = columns;
	}

	@Override
	public PersistenceType getPersistenceType() {
		return PersistenceType.ENTITY;
	}

	@Override
	public PersistentAttributeType getPersistentAttributeType() {
		// assume ManyToOne for now
		return PersistentAttributeType.MANY_TO_ONE;
	}

	@Override
	public EntityPersister<J> getEntityPersister() {
		return entityPersister;
	}

	@Override
	public String getJpaEntityName() {
		return entityPersister.getJpaEntityName();
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return entityPersister.getJavaTypeDescriptor();
	}

	@Override
	public <N> Navigable<N> findNavigable(String navigableName) {
		return entityPersister.findNavigable( navigableName );
	}

	@Override
	public <N> Navigable<N> findDeclaredNavigable(String navigableName) {
		return entityPersister.findDeclaredNavigable( navigableName );
	}

	@Override
	public void visitNavigables(NavigableVisitationStrategy visitor) {
		entityPersister.visitNavigables( visitor );
	}

	@Override
	public void visitDeclaredNavigables(NavigableVisitationStrategy visitor) {
		entityPersister.visitNavigables( visitor );
	}

	@Override
	public List<JoinColumnMapping> resolveJoinColumnMappings(PersistentAttribute persistentAttribute) {
		return null;
	}

	@Override
	public boolean isAssociation() {
		return true;
	}

	public EntityPersister getAssociatedEntityPersister() {
		return entityPersister;
	}

	@Override
	public SingularAttributeClassification getAttributeTypeClassification() {
		return classification;
	}

	public List<Column> getColumns() {
		return columns;
	}

	@Override
	public String asLoggableText() {
		return "SingularAttributeEntity([" + getAttributeTypeClassification().name() + "] " +
				getContainer().asLoggableText() + '.' + getAttributeName() +
				")";
	}

	@Override
	public String toString() {
		return asLoggableText();
	}

	public String getEntityName() {
		return entityPersister.getEntityName();
	}

	@Override
	public List<JoinColumnMapping> getJoinColumnMappings() {
		if ( joinColumnMappings == null ) {
			this.joinColumnMappings = getContainer().resolveJoinColumnMappings( this );
		}
		return joinColumnMappings;
	}

	@Override
	public NavigableRole getNavigableRole() {
		return navigableRole;
	}

	@Override
	public void visitNavigable(NavigableVisitationStrategy visitor) {
		visitor.visitSingularAttributeEntity( this );
	}

	@Override
	public TableGroup buildTableGroup(
			TableSpace tableSpace,
			SqlAliasBaseManager sqlAliasBaseManager,
			FromClauseIndex fromClauseIndex) {
		throw new NotYetImplementedException(  );
	}

	@Override
	public QueryResult generateReturn(
			QueryResultCreationContext returnResolutionContext,
			TableGroup tableGroup) {
		throw new NotYetImplementedException(  );
	}

	@Override
	public Fetch generateFetch(
			QueryResultCreationContext returnResolutionContext,
			TableGroup tableGroup,
			FetchParent fetchParent) {
		throw new NotYetImplementedException(  );
	}
}

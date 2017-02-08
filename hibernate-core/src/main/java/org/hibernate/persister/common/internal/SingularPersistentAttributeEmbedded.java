/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.persister.common.internal;

import java.util.Collections;
import java.util.List;

import org.hibernate.persister.common.NavigableRole;
import org.hibernate.persister.common.spi.AbstractSingularPersistentAttribute;
import org.hibernate.persister.common.spi.Column;
import org.hibernate.persister.common.spi.JoinColumnMapping;
import org.hibernate.persister.common.spi.JoinablePersistentAttribute;
import org.hibernate.persister.common.spi.ManagedTypeImplementor;
import org.hibernate.persister.common.spi.NavigableVisitationStrategy;
import org.hibernate.persister.common.spi.SingularPersistentAttribute;
import org.hibernate.persister.embedded.spi.EmbeddedPersister;
import org.hibernate.persister.embedded.spi.EmbeddedReference;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.sql.ast.from.TableGroup;
import org.hibernate.sql.ast.from.TableSpace;
import org.hibernate.sql.convert.internal.FromClauseIndex;
import org.hibernate.sql.convert.internal.SqlAliasBaseManager;
import org.hibernate.sql.convert.results.spi.Fetch;
import org.hibernate.sql.convert.results.spi.FetchParent;
import org.hibernate.sql.convert.results.spi.Return;
import org.hibernate.sql.convert.results.spi.ReturnResolutionContext;
import org.hibernate.sqm.domain.SqmNavigable;
import org.hibernate.type.spi.EmbeddedType;

/**
 * @author Steve Ebersole
 */
public class SingularPersistentAttributeEmbedded<O,J>
		extends AbstractSingularPersistentAttribute<O,J,EmbeddedType<J>>
		implements SingularPersistentAttribute<O,J>, EmbeddedReference<J>, JoinablePersistentAttribute<O,J> {

	private final EmbeddedPersister<?> embeddablePersister;

	public SingularPersistentAttributeEmbedded(
			ManagedTypeImplementor declaringType,
			String attributeName,
			PropertyAccess propertyAccess,
			Disposition disposition,
			EmbeddedPersister embeddablePersister) {
		super( declaringType, attributeName, propertyAccess, embeddablePersister.getOrmType(), disposition, true );
		this.embeddablePersister = embeddablePersister;
	}

	@Override
	public ManagedTypeImplementor getSource() {
		return super.getSource();
	}

	@Override
	public EmbeddedType getExportedDomainType() {
		return (EmbeddedType) super.getExportedDomainType();
	}

	public EmbeddedPersister getEmbeddablePersister() {
		return embeddablePersister;
	}

	@Override
	public SingularAttributeClassification getAttributeTypeClassification() {
		return SingularAttributeClassification.EMBEDDED;
	}

	@Override
	public List<Column> getColumns() {
		return embeddablePersister.collectColumns();
	}

	@Override
	public String asLoggableText() {
		return toString();
	}

	@Override
	public List<JoinColumnMapping> getJoinColumnMappings() {
		// there are no columns involved in a join to an embedded/composite attribute
		return Collections.emptyList();
	}

	@Override
	public PersistentAttributeType getPersistentAttributeType() {
		return PersistentAttributeType.EMBEDDED;
	}

	@Override
	public PersistenceType getPersistenceType() {
		return PersistenceType.EMBEDDABLE;
	}

	@Override
	public SqmNavigable findNavigable(String navigableName) {
		return getEmbeddablePersister().findNavigable( navigableName );
	}

	@Override
	public NavigableRole getNavigableRole() {
		return embeddablePersister.getNavigableRole();
	}

	@Override
	public void visitNavigable(NavigableVisitationStrategy visitor) {
		visitor.visitSingularAttributeEmbedded( this );
	}

	@Override
	public TableGroup buildTableGroup(
			TableSpace tableSpace, SqlAliasBaseManager sqlAliasBaseManager, FromClauseIndex fromClauseIndex) {
		return null;
	}

	@Override
	public Return generateReturn(
			ReturnResolutionContext returnResolutionContext, TableGroup tableGroup) {
		return null;
	}

	@Override
	public Fetch generateFetch(
			ReturnResolutionContext returnResolutionContext, TableGroup tableGroup, FetchParent fetchParent) {
		return null;
	}
}

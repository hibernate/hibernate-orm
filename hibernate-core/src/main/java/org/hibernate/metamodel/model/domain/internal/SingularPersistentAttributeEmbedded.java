/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.metamodel.model.domain.internal;

import java.util.List;

import org.hibernate.engine.FetchStrategy;
import org.hibernate.mapping.Component;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.model.domain.spi.AbstractSingularPersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.EmbeddedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.EmbeddedValuedNavigable;
import org.hibernate.metamodel.model.domain.spi.ManagedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.metamodel.model.domain.spi.NavigableVisitationStrategy;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.metamodel.model.relational.spi.ForeignKey;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.sql.ast.produce.metamodel.spi.Fetchable;
import org.hibernate.sql.ast.produce.spi.ColumnReferenceQualifier;
import org.hibernate.sql.results.internal.CompositeFetchImpl;
import org.hibernate.sql.results.spi.Fetch;
import org.hibernate.sql.results.spi.FetchParent;
import org.hibernate.sql.results.spi.QueryResultCreationContext;
import org.hibernate.type.descriptor.java.spi.EmbeddableJavaDescriptor;

/**
 * @author Steve Ebersole
 */
public class SingularPersistentAttributeEmbedded<O,J>
		extends AbstractSingularPersistentAttribute<O,J>
		implements EmbeddedValuedNavigable<J>, Fetchable<J> {

	private final EmbeddedTypeDescriptor<J> embeddedDescriptor;

	public SingularPersistentAttributeEmbedded(
			ManagedTypeDescriptor<O> declaringType,
			String attributeName,
			PropertyAccess propertyAccess,
			Disposition disposition,
			Component embeddedMapping,
			RuntimeModelCreationContext context) {
		super( declaringType, attributeName, propertyAccess, disposition, true, embeddedMapping );

		this.embeddedDescriptor = embeddedMapping.makeRuntimeDescriptor(
				declaringType,
				attributeName,
				context
		);
	}

	@Override
	public ManagedTypeDescriptor<O> getContainer() {
		return super.getContainer();
	}

	@Override
	public EmbeddedTypeDescriptor<J> getEmbeddedDescriptor() {
		return embeddedDescriptor;
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
		return embeddedDescriptor.collectColumns();
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
		return this.getEmbeddedDescriptor().findNavigable( navigableName );
	}

	@Override
	public NavigableRole getNavigableRole() {
		return embeddedDescriptor.getNavigableRole();
	}

	@Override
	public void visitNavigable(NavigableVisitationStrategy visitor) {
		visitor.visitSingularAttributeEmbedded( this );
	}

	@Override
	public <N> Navigable<N> findDeclaredNavigable(String navigableName) {
		return embeddedDescriptor.findDeclaredNavigable( navigableName );
	}

	@Override
	public List<Navigable> getNavigables() {
		return embeddedDescriptor.getNavigables();
	}

	@Override
	public List<Navigable> getDeclaredNavigables() {
		return embeddedDescriptor.getDeclaredNavigables();
	}

	@Override
	public void visitNavigables(NavigableVisitationStrategy visitor) {
		embeddedDescriptor.visitNavigables( visitor );
	}

	@Override
	public void visitDeclaredNavigables(NavigableVisitationStrategy visitor) {
		embeddedDescriptor.visitDeclaredNavigables( visitor );
	}

	@Override
	public Fetch generateFetch(
			FetchParent fetchParent,
			ColumnReferenceQualifier qualifier,
			FetchStrategy fetchStrategy,
			String resultVariable,
			QueryResultCreationContext creationContext) {
		// todo (6.0) : use qualifier to create the SqlSelection mappings
		return new CompositeFetchImpl(
				fetchParent,
				qualifier,
				this,
				fetchStrategy,
				creationContext
		);
	}

	@Override
	public FetchStrategy getMappedFetchStrategy() {
		throw new NotYetImplementedFor6Exception(  );
	}

	@Override
	public ManagedTypeDescriptor<J> getFetchedManagedType() {
		return embeddedDescriptor;
	}

	@Override
	public ForeignKey.ColumnMappings getJoinColumnMappings() {
		throw new NotYetImplementedFor6Exception(  );
	}
}

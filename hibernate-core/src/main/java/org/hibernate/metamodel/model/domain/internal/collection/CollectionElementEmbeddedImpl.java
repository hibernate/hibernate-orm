/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal.collection;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.persistence.TemporalType;

import org.hibernate.boot.model.domain.spi.EmbeddedValueMappingImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.mapping.Collection;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.spi.AbstractCollectionElement;
import org.hibernate.metamodel.model.domain.spi.AllowableParameterType;
import org.hibernate.metamodel.model.domain.spi.CollectionElementEmbedded;
import org.hibernate.metamodel.model.domain.spi.EmbeddedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.metamodel.model.domain.spi.NavigableVisitationStrategy;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.metamodel.model.domain.spi.SingularPersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.TableReferenceJoinCollector;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.procedure.ParameterMisuseException;
import org.hibernate.query.sqm.produce.spi.SqmCreationContext;
import org.hibernate.query.sqm.tree.expression.domain.SqmCollectionElementReferenceEmbedded;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableContainerReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmPluralAttributeReference;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.JoinType;
import org.hibernate.sql.ast.produce.metamodel.spi.Fetchable;
import org.hibernate.sql.ast.produce.spi.ColumnReferenceQualifier;
import org.hibernate.sql.ast.produce.spi.SqlAliasBase;
import org.hibernate.sql.ast.produce.spi.TableReferenceContributor;
import org.hibernate.type.descriptor.java.spi.EmbeddableJavaDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public class CollectionElementEmbeddedImpl<J>
		extends AbstractCollectionElement<J>
		implements CollectionElementEmbedded<J> {

	private final EmbeddedTypeDescriptor<J> embeddedDescriptor;

	public CollectionElementEmbeddedImpl(
			PersistentCollectionDescriptor descriptor,
			Collection mapping,
			RuntimeModelCreationContext creationContext) {
		super( descriptor );

		this.embeddedDescriptor = creationContext.getRuntimeModelDescriptorFactory().createEmbeddedTypeDescriptor(
				(EmbeddedValueMappingImplementor) mapping.getElement(),
				descriptor,
				null,
				NAVIGABLE_NAME,
				SingularPersistentAttribute.Disposition.NORMAL,
				creationContext
		);
	}

	@Override
	public EmbeddedTypeDescriptor<J> getEmbeddedDescriptor() {
		return embeddedDescriptor;
	}

	@Override
	public EmbeddableJavaDescriptor<J> getJavaTypeDescriptor() {
		return getEmbeddedDescriptor().getJavaTypeDescriptor();
	}

	@Override
	public <N> Navigable<N> findNavigable(String navigableName) {
		return getEmbeddedDescriptor().findNavigable( navigableName );
	}

	@Override
	public void visitNavigables(NavigableVisitationStrategy visitor) {
		// visit our sub-navigables
		getEmbeddedDescriptor().visitNavigables( visitor );
	}

	@Override
	public SqmNavigableReference createSqmExpression(
			SqmFrom sourceSqmFrom,
			SqmNavigableContainerReference containerReference,
			SqmCreationContext creationContext) {
		return new SqmCollectionElementReferenceEmbedded( (SqmPluralAttributeReference) containerReference );
	}

	@Override
	public void visitColumns(
			BiConsumer<SqlExpressableType, Column> action,
			Clause clause,
			TypeConfiguration typeConfiguration) {
		getEmbeddedDescriptor().visitColumns( action, clause, typeConfiguration );
	}

	@Override
	public Object unresolve(Object value, SharedSessionContractImplementor session) {
		return getEmbeddedDescriptor().unresolve( value, session );
	}

	@Override
	public void dehydrate(
			Object value,
			JdbcValueCollector jdbcValueCollector,
			Clause clause,
			SharedSessionContractImplementor session) {
		getEmbeddedDescriptor().dehydrate( value, jdbcValueCollector, clause, session );
	}

	@Override
	public void visitFetchables(Consumer<Fetchable> fetchableConsumer) {
		getEmbeddedDescriptor().visitFetchables( fetchableConsumer );
	}

	@Override
	public AllowableParameterType resolveTemporalPrecision(TemporalType temporalType, TypeConfiguration typeConfiguration) {
		throw new ParameterMisuseException( "Cannot apply temporal precision to embeddable value" );
	}

	@Override
	public void applyTableReferenceJoins(
			ColumnReferenceQualifier lhs,
			JoinType joinType,
			SqlAliasBase sqlAliasBase,
			TableReferenceJoinCollector joinCollector) {
		getEmbeddedDescriptor().visitStateArrayContributors(
				contributor -> {
					if ( contributor instanceof TableReferenceContributor ) {
						( (TableReferenceContributor) contributor ).applyTableReferenceJoins( lhs, joinType, sqlAliasBase, joinCollector );
					}
				}
		);
	}

	@Override
	public EmbeddedTypeDescriptor<J> getDomainTypeDescriptor() {
		return getEmbeddedDescriptor();
	}
}

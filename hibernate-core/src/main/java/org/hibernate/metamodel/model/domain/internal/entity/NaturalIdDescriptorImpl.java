/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import javax.persistence.TemporalType;

import org.hibernate.cache.spi.access.NaturalIdDataAccess;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.model.domain.spi.AllowableParameterType;
import org.hibernate.metamodel.model.domain.spi.EntityHierarchy;
import org.hibernate.metamodel.model.domain.spi.NaturalIdDescriptor;
import org.hibernate.metamodel.model.domain.spi.NavigableContainer;
import org.hibernate.metamodel.model.domain.spi.NavigableVisitationStrategy;
import org.hibernate.metamodel.model.domain.spi.NonIdPersistentAttribute;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.produce.spi.ColumnReferenceQualifier;
import org.hibernate.sql.ast.produce.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.tree.spi.expression.ColumnReference;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;
import org.hibernate.type.descriptor.java.spi.TemporalJavaDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public class NaturalIdDescriptorImpl<J> implements NaturalIdDescriptor<J>, AllowableParameterType<J> {
	private final EntityHierarchy hierarchy;
	private final NaturalIdDataAccess cacheRegionAccess;
	private final NavigableRole navigableRole;

	private List<NaturalIdAttributeInfo> attributes;

	private Integer numberOfParameterBinds;


	public NaturalIdDescriptorImpl(
			EntityHierarchy hierarchy,
			NaturalIdDataAccess cacheRegionAccess) {
		this.hierarchy = hierarchy;
		this.cacheRegionAccess = cacheRegionAccess;

		navigableRole = hierarchy.getRootEntityType().getNavigableRole().append( "naturalId" );
	}

	public void injectAttributes(List<NonIdPersistentAttribute<?,?>> attributes) {
		if ( this.attributes == null ) {
			this.attributes = new ArrayList<>();
			this.numberOfParameterBinds = 0;
		}

		for ( int i = 0; i < attributes.size(); i++ ) {
			this.attributes.add( new NaturalIdAttributeInfoImpl( attributes.get( i ), i ) );
			this.numberOfParameterBinds += ( (AllowableParameterType) attributes.get( i ) ).getNumberOfJdbcParametersNeeded();
		}
	}

	@Override
	public List<NaturalIdAttributeInfo> getAttributeInfos() {
		return attributes;
	}

	@Override
	public boolean isMutable() {
		return false;
	}

	@Override
	public Object[] resolveSnapshot(Object entityId, SharedSessionContractImplementor session) {
		return new Object[0];
	}

	@Override
	public NaturalIdDataAccess getCacheAccess() {
		return cacheRegionAccess;
	}

	public static class NaturalIdAttributeInfoImpl implements NaturalIdAttributeInfo {
		private final NonIdPersistentAttribute underlyingAttribute;
		private final int stateArrayPosition;

		public NaturalIdAttributeInfoImpl(NonIdPersistentAttribute underlyingAttribute, int stateArrayPosition) {
			this.underlyingAttribute = underlyingAttribute;
			this.stateArrayPosition = stateArrayPosition;
		}

		@Override
		public NonIdPersistentAttribute getUnderlyingAttributeDescriptor() {
			return underlyingAttribute;
		}

		@Override
		public int getStateArrayPosition() {
			return stateArrayPosition;
		}
	}

	@Override
	public NavigableContainer getContainer() {
		return hierarchy.getRootEntityType();
	}

	@Override
	public NavigableRole getNavigableRole() {
		return navigableRole;
	}

	@Override
	public void visitNavigable(NavigableVisitationStrategy visitor) {
		attributes.forEach(
				attributeInfo -> attributeInfo.getUnderlyingAttributeDescriptor()
						.visitNavigable( visitor )
		);
	}

	@Override
	public List<ColumnReference> resolveColumnReferences(
			ColumnReferenceQualifier qualifier,
			SqlAstCreationContext creationContext) {
		final List<ColumnReference> columnRefs = new ArrayList<>();
		attributes.forEach( attributeInfo -> columnRefs.addAll( attributeInfo.getUnderlyingAttributeDescriptor().resolveColumnReferences( qualifier, creationContext ) ) );
		return columnRefs;
	}

	@Override
	public String asLoggableText() {
		return "NaturalId (" + hierarchy.getRootEntityType().getEntityName() + ")";
	}

	@Override
	public JavaTypeDescriptor<J> getJavaTypeDescriptor() {
		return (JavaTypeDescriptor<J>) hierarchy.getRootEntityType().getJavaTypeDescriptor();
	}

	@Override
	public PersistenceType getPersistenceType() {
		return attributes.size() > 1 ? PersistenceType.EMBEDDABLE : PersistenceType.BASIC;
	}

	@Override
	public int getNumberOfJdbcParametersNeeded() {
		return numberOfParameterBinds;
	}

	public AllowableParameterType resolveTemporalPrecision(
			TemporalType temporalType,
			TypeConfiguration typeConfiguration) {
		if ( attributes.size() == 1 ) {
			final NaturalIdAttributeInfo naturalIdAttributeInfo = attributes.get( 0 );
			if ( naturalIdAttributeInfo.getUnderlyingAttributeDescriptor() instanceof TemporalJavaDescriptor ) {
				final TemporalJavaDescriptor jtd = (TemporalJavaDescriptor) naturalIdAttributeInfo.getUnderlyingAttributeDescriptor().getJavaTypeDescriptor();
				return (AllowableParameterType) jtd.resolveTypeForPrecision( temporalType, typeConfiguration );
			}
		}

		throw new UnsupportedOperationException( "Composite natural-id cannot be treated as a temporal value" );
	}


	@Override
	public void visitColumns(
			BiConsumer<SqlExpressableType, Column> action,
			Clause clause,
			TypeConfiguration typeConfiguration) {
		visitPersistentAttributes(
				naturalIdAttributeInfo -> naturalIdAttributeInfo.getUnderlyingAttributeDescriptor().visitColumns(
						action,
						clause,
						typeConfiguration
				)
		);
	}
}

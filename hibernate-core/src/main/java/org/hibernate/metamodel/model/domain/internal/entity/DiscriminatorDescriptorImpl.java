/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.metamodel.model.domain.internal.entity;

import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.model.domain.spi.BasicValueMapper;
import org.hibernate.metamodel.model.domain.spi.DiscriminatorDescriptor;
import org.hibernate.metamodel.model.domain.spi.DiscriminatorMappings;
import org.hibernate.metamodel.model.domain.spi.EntityHierarchy;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.produce.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.domain.SqmBasicValuedSimplePath;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableContainerReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableReference;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.sql.SqlExpressableType;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.spi.BasicType;

/**
 * @author Steve Ebersole
 */
public class DiscriminatorDescriptorImpl<O,J> implements DiscriminatorDescriptor<J> {
	public static final String NAVIGABLE_NAME = "{discriminator}";

	private final EntityHierarchy hierarchy;
	private final BasicType basicType;
	private final BasicValueMapper<J> valueMapper;
	private final Column column;

	private final NavigableRole navigableRole;

	@SuppressWarnings("WeakerAccess")
	public DiscriminatorDescriptorImpl(
			EntityHierarchy hierarchy,
			org.hibernate.boot.model.domain.BasicValueMapping<J> valueMapping,
			RuntimeModelCreationContext creationContext) {
		this.hierarchy = hierarchy;

		this.basicType = valueMapping.getResolution().getBasicType();
		this.valueMapper = valueMapping.getResolution().getValueMapper();

		this.column = creationContext.getDatabaseObjectResolver().resolveColumn( valueMapping.getMappedColumn() );

		this.navigableRole = hierarchy.getRootEntityType().getNavigableRole().append( NAVIGABLE_NAME );
	}

	@Override
	public EntityTypeDescriptor<O> getContainer() {
		return hierarchy.getRootEntityType();
	}

	@Override
	public NavigableRole getNavigableRole() {
		return navigableRole;
	}

	@Override
	public String getNavigableName() {
		return NAVIGABLE_NAME;
	}

	@Override
	public BasicJavaDescriptor<J> getJavaTypeDescriptor() {
		return getValueMapper().getDomainJavaDescriptor();
	}

	@Override
	public DiscriminatorMappings getDiscriminatorMappings() {
		// todo (6.0) : will probably need to collect these dynamically during "first phase" of runtime model creation
		throw new NotYetImplementedException(  );
	}

	@Override
	public String asLoggableText() {
		return getContainer().asLoggableText() + '.' + NAVIGABLE_NAME;
	}

	@Override
	public PersistenceType getPersistenceType() {
		return PersistenceType.BASIC;
	}

	@Override
	public Column getBoundColumn() {
		return column;
	}

	@Override
	public BasicValueMapper<J> getValueMapper() {
		return valueMapper;
	}

	@Override
	public SqlExpressableType getSqlExpressableType() {
		return valueMapper.getSqlExpressableType();
	}

	@Override
	public SqmNavigableReference createSqmExpression(
			SqmFrom sourceSqmFrom,
			SqmNavigableContainerReference containerReference,
			SqmCreationState creationState) {
		return new SqmBasicValuedSimplePath(
				creationState.generateUniqueIdentifier(),
				new NavigablePath( getContainer().getNavigableName() + NAVIGABLE_NAME ),
				this,
				null
		);
	}
}

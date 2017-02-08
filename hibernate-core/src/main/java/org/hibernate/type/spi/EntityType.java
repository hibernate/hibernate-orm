/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.spi;

import org.hibernate.MappingException;
import org.hibernate.persister.entity.spi.EntityPersister;
import org.hibernate.sqm.domain.type.SqmDomainTypeEntity;
import org.hibernate.type.descriptor.java.spi.EntityJavaDescriptor;

/**
 * @author Steve Ebersole
 */
public interface EntityType<E> extends IdentifiableType<E>, SqmDomainTypeEntity {
	EntityPersister<E> getEntityPersister();

	@Override
	EntityJavaDescriptor<E> getJavaTypeDescriptor();

	@Override
	default Class<E> getJavaType() {
		return getJavaTypeDescriptor().getJavaType();
	}

	default String getEntityName() {
		return getJavaTypeDescriptor().getEntityName();
	}

	default String getJpaEntityName() {
		return getJavaTypeDescriptor().getJpaEntityName();
	}

	@Override
	default JdbcLiteralFormatter getJdbcLiteralFormatter() {
		return null;
	}



	/**
	 * Is the association modeled here defined as a 1-1 in the database (physical model)?
	 *
	 * @return True if a 1-1 in the database; false otherwise.
	 */
	boolean isOneToOne();

	/**
	 * Is the association modeled here a 1-1 according to the logical moidel?
	 *
	 * @return True if a 1-1 in the logical model; false otherwise.
	 */
	boolean isLogicalOneToOne();

	/**
	 * The name of the associated entity.
	 *
	 * @return The associated entity name.
	 */
	String getAssociatedEntityName();

	/**
	 * Determine the type of either (1) the identifier if we reference the
	 * associated entity's PK or (2) the unique key to which we refer (i.e.
	 * the property-ref).
	 *
	 * @return The appropriate type.
	 *
	 * @throws MappingException Generally, if unable to resolve the associated entity name
	 * or unique key property name.
	 */
	Type getIdentifierOrUniqueKeyType() throws MappingException;

	/**
	 * Does this association foreign key reference the primary key of the other table?
	 * Otherwise, it references a property-ref.
	 *
	 * @return True if this association reference the PK of the associated entity.
	 */
	boolean isReferenceToPrimaryKey();

	/**
	 * The name of the property on the associated entity to which our FK
	 * refers
	 *
	 * @return The appropriate property name.
	 *
	 * @throws MappingException Generally, if unable to resolve the associated entity name
	 */
	String getIdentifierOrUniqueKeyPropertyName();

	public String getPropertyName();
}

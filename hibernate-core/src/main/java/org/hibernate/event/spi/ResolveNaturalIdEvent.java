/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.spi;

import java.util.Collections;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.NaturalIdDescriptor;
import org.hibernate.metamodel.model.domain.spi.NaturalIdDescriptor.NaturalIdAttributeInfo;

/**
 * Defines an event class for the resolving of an entity id from the entity's natural-id
 * 
 * @author Eric Dalquist
 * @author Steve Ebersole
 */
public class ResolveNaturalIdEvent extends AbstractEvent {
	public static final LockMode DEFAULT_LOCK_MODE = LockMode.NONE;

	private final EntityTypeDescriptor entityDescriptor;
	private final Map<String, Object> naturalIdValues;
	private final Object[] orderedNaturalIdValues;
	private final LockOptions lockOptions;

	private Object entityId;

	public ResolveNaturalIdEvent(
			Map<String, Object> naturalIdValues,
			EntityTypeDescriptor entityDescriptor,
			EventSource source) {
		this( naturalIdValues, entityDescriptor, new LockOptions(), source );
	}

	public ResolveNaturalIdEvent(
			Map<String, Object> naturalIdValues,
			EntityTypeDescriptor entityDescriptor,
			LockOptions lockOptions,
			EventSource source) {
		super( source );

		if ( entityDescriptor == null ) {
			throw new IllegalArgumentException( "EntityPersister is required for loading" );
		}

		if ( ! entityDescriptor.hasNaturalIdentifier() ) {
			throw new HibernateException( "Entity did not define a natural-id" );
		}

		if ( naturalIdValues == null || naturalIdValues.isEmpty() ) {
			throw new IllegalArgumentException( "natural-id to load is required" );
		}

		final NaturalIdDescriptor<?> naturalIdDescriptor = entityDescriptor.getHierarchy().getNaturalIdDescriptor();
		if ( naturalIdDescriptor.getAttributeInfos().size() != naturalIdValues.size() ) {
			throw new HibernateException(
					String.format(
							"Entity [%s] defines its natural-id with %d properties but only %d were specified",
							entityDescriptor.getEntityName(),
							naturalIdDescriptor.getAttributeInfos().size(),
							naturalIdValues.size()
					)
			);
		}

		if ( lockOptions.getLockMode() == LockMode.WRITE ) {
			throw new IllegalArgumentException( "Invalid lock mode for loading" );
		}
		else if ( lockOptions.getLockMode() == null ) {
			lockOptions.setLockMode( DEFAULT_LOCK_MODE );
		}

		this.entityDescriptor = entityDescriptor;
		this.naturalIdValues = naturalIdValues;
		this.lockOptions = lockOptions;

		this.orderedNaturalIdValues = new Object[ naturalIdDescriptor.getAttributeInfos().size() ];

		int i = 0;
		for ( NaturalIdAttributeInfo naturalIdAttribute : naturalIdDescriptor.getAttributeInfos() ) {
			// todo (6.0) : still need to figure out ordering of array elements for "value arrays"
			final String attributeName = naturalIdAttribute.getUnderlyingAttributeDescriptor().getName();
			if ( ! naturalIdValues.containsKey( attributeName ) ) {
				throw new HibernateException(
						String.format(
								"No value specified for natural-id property %s#%s",
								getEntityName(),
								attributeName
						)
				);
			}
			orderedNaturalIdValues[i++] = naturalIdValues.get( attributeName );
		}
	}

	public Map<String, Object> getNaturalIdValues() {
		return Collections.unmodifiableMap( naturalIdValues );
	}

	public Object[] getOrderedNaturalIdValues() {
		return orderedNaturalIdValues;
	}

	public EntityTypeDescriptor getEntityDescriptor() {
		return entityDescriptor;
	}

	public String getEntityName() {
		return getEntityDescriptor().getEntityName();
	}

	public LockOptions getLockOptions() {
		return lockOptions;
	}

	public Object getEntityId() {
		return entityId;
	}

	public void setEntityId(Object entityId) {
		this.entityId = entityId;
	}
}

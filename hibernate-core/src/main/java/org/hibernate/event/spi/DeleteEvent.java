/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;


/**
 * Event class for {@link org.hibernate.Session#remove}.
 *
 * @apiNote This class predates JPA, and today should
 *          really be named {@code RemoveEvent}.
 *
 * @author Steve Ebersole
 *
 * @see org.hibernate.Session#remove
 */
public class DeleteEvent extends AbstractSessionEvent {
	private final Object object;
	private String entityName;
	private boolean cascadeDeleteEnabled;
	// TODO: The removeOrphan concept is a temporary "hack" for HHH-6484.
	//       This should be removed once action/task ordering is improved.
	private boolean orphanRemovalBeforeUpdates;

	public DeleteEvent(@Nonnull Object object, @Nonnull EventSource source) {
		super(source);
		if (object == null) {
			throw new IllegalArgumentException( "Entity may not be null" );
		}
		this.object = object;
	}

	public DeleteEvent(@Nullable String entityName, @Nonnull Object object, @Nonnull EventSource source) {
		this(object, source);
		this.entityName = entityName;
	}

	public DeleteEvent(@Nullable String entityName, @Nonnull Object object, boolean cascadeDeleteEnabled, @Nonnull EventSource source) {
		this(object, source);
		this.entityName = entityName;
		this.cascadeDeleteEnabled = cascadeDeleteEnabled;
	}

	public DeleteEvent(@Nullable String entityName, @Nonnull Object object, boolean cascadeDeleteEnabled,
			boolean orphanRemovalBeforeUpdates, @Nonnull EventSource source) {
		this(object, source);
		this.entityName = entityName;
		this.cascadeDeleteEnabled = cascadeDeleteEnabled;
		this.orphanRemovalBeforeUpdates = orphanRemovalBeforeUpdates;
	}

	/**
	 * Returns the encapsulated entity to be deleted.
	 *
	 * @return The entity to be deleted.
	 */
	@Nonnull
	public Object getObject() {
		return object;
	}

	@Nullable
	public String getEntityName() {
		return entityName;
	}

	public boolean isCascadeDeleteEnabled() {
		return cascadeDeleteEnabled;
	}

	public boolean isOrphanRemovalBeforeUpdates() {
		return orphanRemovalBeforeUpdates;
	}

}

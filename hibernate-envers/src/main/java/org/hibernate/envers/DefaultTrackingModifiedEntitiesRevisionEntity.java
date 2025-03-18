/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers;

import jakarta.persistence.Entity;

/**
 * Extension of standard {@link DefaultRevisionEntity} that allows tracking entity names changed in each revision.
 * This revision entity is implicitly used when {@code org.hibernate.envers.track_entities_changed_in_revision}
 * parameter is set to {@code true}.
 *
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Entity
public final class DefaultTrackingModifiedEntitiesRevisionEntity extends TrackingModifiedEntitiesRevisionMapping {
}

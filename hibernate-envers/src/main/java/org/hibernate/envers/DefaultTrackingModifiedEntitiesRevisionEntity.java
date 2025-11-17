/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers;

/**
 * Extension of standard {@link DefaultRevisionEntity} that allows tracking entity names changed in each revision.
 * This revision entity is implicitly used when {@code org.hibernate.envers.track_entities_changed_in_revision}
 * parameter is set to {@code true}.
 *
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public final class DefaultTrackingModifiedEntitiesRevisionEntity extends TrackingModifiedEntitiesRevisionMapping {
}

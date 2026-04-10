/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpa.event.spi;

import java.lang.annotation.Annotation;

import jakarta.persistence.PostDelete;
import jakarta.persistence.PostInsert;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PostUpsert;
import jakarta.persistence.PreDelete;
import jakarta.persistence.PreInsert;
import jakarta.persistence.PreMerge;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.PreUpsert;

/// Enumerates the Jakarta Persistence style lifecycle callback types.
///
/// @author Steve Ebersole
public enum CallbackType {

	/// @see PreMerge
	PRE_MERGE,

	/// @see PrePersist
	PRE_PERSIST,

	/// @see PostPersist
	POST_PERSIST,

	/// @see PreUpdate
	PRE_UPDATE,

	/// @see PostUpdate
	POST_UPDATE,

	/// @see PreUpsert
	PRE_UPSERT,

	/// @see PostUpsert
	POST_UPSERT,

	/// @see PreInsert
	PRE_INSERT,

	/// @see PostInsert
	POST_INSERT,

	/// @see PreRemove
	PRE_REMOVE,

	/// @see PostRemove
	POST_REMOVE,

	/// @see PreDelete
	PRE_DELETE,

	/// @see PostDelete
	POST_DELETE,

	/// @see PostLoad
	POST_LOAD;

	/// Returns the callback type for a given callback annotation.
	public static CallbackType fromCallbackAnnotation(Class<? extends Annotation> ann) {
		if ( PreMerge.class.equals( ann ) ) {
			return PRE_MERGE;
		}
		if ( PrePersist.class.equals( ann ) ) {
			return PRE_PERSIST;
		}
		if ( PostPersist.class.equals( ann ) ) {
			return POST_PERSIST;
		}
		if ( PreUpdate.class.equals( ann ) ) {
			return PRE_UPDATE;
		}
		if ( PostUpdate.class.equals( ann ) ) {
			return POST_UPDATE;
		}
		if ( PreUpsert.class.equals( ann ) ) {
			return PRE_UPSERT;
		}
		if ( PostUpsert.class.equals( ann ) ) {
			return POST_UPSERT;
		}
		if ( PreInsert.class.equals( ann ) ) {
			return PRE_INSERT;
		}
		if ( PostInsert.class.equals( ann ) ) {
			return POST_INSERT;
		}
		if ( PreRemove.class.equals( ann ) ) {
			return PRE_REMOVE;
		}
		if ( PostRemove.class.equals( ann ) ) {
			return POST_REMOVE;
		}
		if ( PreDelete.class.equals( ann ) ) {
			return PRE_DELETE;
		}
		if ( PostDelete.class.equals( ann ) ) {
			return POST_DELETE;
		}
		if ( PostLoad.class.equals( ann ) ) {
			return POST_LOAD;
		}
		throw new IllegalArgumentException( "Unknown callback annotation : " + ann );
	}

	/// The callback annotation type corresponding to this lifecycle callback type.
	public Class<? extends Annotation> getCallbackAnnotation() {
		return switch ( this ) {
			case PRE_MERGE -> PreMerge.class;
			case PRE_PERSIST -> PrePersist.class;
			case PRE_UPDATE -> PreUpdate.class;
			case PRE_UPSERT -> PreUpsert.class;
			case PRE_INSERT -> PreInsert.class;
			case PRE_REMOVE -> PreRemove.class;
			case PRE_DELETE -> PreDelete.class;
			case POST_PERSIST -> PostPersist.class;
			case POST_UPDATE -> PostUpdate.class;
			case POST_UPSERT -> PostUpsert.class;
			case POST_INSERT -> PostInsert.class;
			case POST_REMOVE -> PostRemove.class;
			case POST_DELETE -> PostDelete.class;
			case POST_LOAD -> PostLoad.class;
		};
	}
}

package org.hibernate.event.internal;

import jakarta.annotation.Nullable;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.log.SubSystemLogging;
import org.hibernate.persister.entity.EntityPersister;
import org.jboss.logging.Logger;

import static org.hibernate.pretty.MessageHelper.infoString;

/// Logging specifically for the case where a deleted entity is being re-persisted due to cascade.
/// This is a specific requirement of JPA.
///
/// @since 8.0
/// @author Steve Ebersole
public class UnscheduledDeletionLogging {
	private static final Logger UNSCHEDULED_DELETION_LOGGER = Logger.getLogger( UnscheduledDeletionLogging.class, SubSystemLogging.BASE + ".event.unschedule-deletion" );

	public static void unschedulingEntityDeletion(
			@Nullable EntityPersister persister,
			@Nullable Object id,
			@Nullable SessionFactoryImplementor factory) {
		if ( UNSCHEDULED_DELETION_LOGGER.isDebugEnabled() ) {
			UNSCHEDULED_DELETION_LOGGER.debugf( "Unscheduling entity deletion %s",
					infoString( persister, id, factory ) );
		}
	}


}

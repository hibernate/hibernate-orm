/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.internal;

import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.ReplicationMode;
import org.hibernate.TransientObjectException;
import org.hibernate.engine.internal.Cascade;
import org.hibernate.engine.internal.CascadePoint;
import org.hibernate.engine.spi.CascadingAction;
import org.hibernate.engine.spi.CascadingActions;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.ReplicateEvent;
import org.hibernate.event.spi.ReplicateEventListener;
import org.hibernate.id.PostInsertIdentifierGenerator;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityIdentifier;
import org.hibernate.metamodel.model.domain.spi.PersistentAttributeDescriptor;
import org.hibernate.metamodel.model.domain.spi.VersionDescriptor;
import org.hibernate.pretty.MessageHelper;

/**
 * Defines the default replicate event listener used by Hibernate to replicate
 * entities in response to generated replicate events.
 *
 * @author Steve Ebersole
 */
public class DefaultReplicateEventListener extends AbstractSaveEventListener implements ReplicateEventListener {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( DefaultReplicateEventListener.class );

	/**
	 * Handle the given replicate event.
	 *
	 * @param event The replicate event to be handled.
	 *
	 * @throws TransientObjectException An invalid attempt to replicate a transient entity.
	 */
	public void onReplicate(ReplicateEvent event) {
		final EventSource source = event.getSession();
		if ( source.getPersistenceContext().reassociateIfUninitializedProxy( event.getObject() ) ) {
			LOG.trace( "Uninitialized proxy passed to replicate()" );
			return;
		}

		Object entity = source.getPersistenceContext().unproxyAndReassociate( event.getObject() );

		if ( source.getPersistenceContext().isEntryFor( entity ) ) {
			LOG.trace( "Ignoring persistent instance passed to replicate()" );
			//hum ... should we cascade anyway? throw an exception? fine like it is?
			return;
		}

		final EntityTypeDescriptor entityDescriptor = source.getEntityDescriptor( event.getEntityName(), entity );
		final EntityIdentifier idDescriptor = entityDescriptor.getHierarchy().getIdentifierDescriptor();
		final VersionDescriptor versionDescriptor = entityDescriptor.getHierarchy().getVersionDescriptor();

		// todo (6.0) : move methods like `#getIdentifier`, `#getCurrentVersion`, etc from `EntityDescriptor` to `EntityIdentifier`, `VersionDescriptor`, etc

		// get the id from the object
		/*if ( entityDescriptor.isUnsaved(entity, source) ) {
			throw new TransientObjectException("transient instance passed to replicate()");
		}*/
		Object id = entityDescriptor.getIdentifier( entity, source );
		if ( id == null ) {
			throw new TransientObjectException( "instance with null id passed to replicate()" );
		}

		final ReplicationMode replicationMode = event.getReplicationMode();

		final Object oldVersion;
		if ( replicationMode == ReplicationMode.EXCEPTION ) {
			//always do an INSERT, and let it fail by constraint violation
			oldVersion = null;
		}
		else {
			//what is the version on the database?
			oldVersion = entityDescriptor.getHierarchy().getVersionDescriptor().getPropertyAccess().getGetter().get( id );
		}

		final boolean traceEnabled = LOG.isTraceEnabled();
		if ( oldVersion != null ) {
			if ( traceEnabled ) {
				LOG.tracev(
						"Found existing row for {0}", MessageHelper.infoString(
						entityDescriptor,
						id,
						source.getFactory()
				)
				);
			}

			/// HHH-2378
			final Object realOldVersion = versionDescriptor != null ? oldVersion : null;

			boolean canReplicate = replicationMode.shouldOverwriteCurrentVersion(
					entity,
					realOldVersion,
					entityDescriptor.getVersion( entity ),
					versionDescriptor
			);

			// if can replicate, will result in a SQL UPDATE
			// else do nothing (don't even reassociate object!)
			if ( canReplicate ) {
				performReplication( entity, id, realOldVersion, entityDescriptor, replicationMode, source );
			}
			else if ( traceEnabled ) {
				LOG.trace( "No need to replicate" );
			}

			//TODO: would it be better to do a refresh from db?
		}
		else {
			// no existing row - do an insert
			if ( traceEnabled ) {
				LOG.tracev(
						"No existing row, replicating new instance {0}",
						MessageHelper.infoString( entityDescriptor, id, source.getFactory() )
				);
			}

			// prefer re-generation of insert-generated identifiers
			final boolean regenerate = PostInsertIdentifierGenerator.class.isInstance( idDescriptor.getIdentifierValueGenerator() );
			final EntityKey key = regenerate ? null : source.generateEntityKey( id, entityDescriptor );

			performSaveOrReplicate(
					entity,
					key,
					entityDescriptor,
					regenerate,
					replicationMode,
					source,
					true
			);

		}
	}

	@Override
	protected boolean visitCollectionsBeforeSave(
			Object entity,
			Object id,
			Object[] values,
			List<PersistentAttributeDescriptor> navigables,
			EventSource source) {
		//TODO: we use two visitors here, inefficient!
		OnReplicateVisitor visitor = new OnReplicateVisitor( source, id, entity, false );
		visitor.processEntityPropertyValues( values, navigables );
		return super.visitCollectionsBeforeSave( entity, id, values, navigables, source );
	}

	@Override
	protected boolean substituteValuesIfNecessary(
			Object entity,
			Object id,
			Object[] values,
			EntityTypeDescriptor entityDescriptor,
			SessionImplementor source) {
		return false;
	}

	@Override
	protected boolean isVersionIncrementDisabled() {
		return true;
	}

	private void performReplication(
			Object entity,
			Object id,
			Object version,
			EntityTypeDescriptor entityDescriptor,
			ReplicationMode replicationMode,
			EventSource source) throws HibernateException {

		if ( LOG.isTraceEnabled() ) {
			LOG.tracev( "Replicating changes to {0}", MessageHelper.infoString( entityDescriptor, id, source.getFactory() ) );
		}

		new OnReplicateVisitor( source, id, entity, true ).process( entity, entityDescriptor );

		source.getPersistenceContext().addEntity(
				entity,
				( entityDescriptor.getJavaTypeDescriptor().getMutabilityPlan().isMutable() ? Status.MANAGED : Status.READ_ONLY ),
				null,
				source.generateEntityKey( id, entityDescriptor ),
				version,
				LockMode.NONE,
				true,
				entityDescriptor,
				true
		);

		cascadeAfterReplicate( entity, entityDescriptor, replicationMode, source );
	}

	private void cascadeAfterReplicate(
			Object entity,
			EntityTypeDescriptor entityDescriptor,
			ReplicationMode replicationMode,
			EventSource source) {
		source.getPersistenceContext().incrementCascadeLevel();
		try {
			Cascade.cascade(
					CascadingActions.REPLICATE,
					CascadePoint.AFTER_UPDATE,
					source,
					entityDescriptor,
					entity,
					replicationMode
			);
		}
		finally {
			source.getPersistenceContext().decrementCascadeLevel();
		}
	}

	@Override
	protected CascadingAction getCascadeAction() {
		return CascadingActions.REPLICATE;
	}
}

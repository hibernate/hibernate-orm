/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.boot.internal;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.Transaction;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.envers.boot.AuditMetadata;
import org.hibernate.envers.boot.spi.AuditServiceImplementor;
import org.hibernate.envers.boot.spi.AuditServiceOptions;
import org.hibernate.envers.internal.entities.EntitiesConfigurations;
import org.hibernate.envers.internal.revisioninfo.ModifiedEntityNamesReader;
import org.hibernate.envers.internal.revisioninfo.RevisionInfoNumberReader;
import org.hibernate.envers.internal.revisioninfo.RevisionInfoQueryCreator;
import org.hibernate.envers.internal.synchronization.AuditProcess;
import org.hibernate.event.spi.EventSource;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * @author Chris Cranford
 * @since 6.0
 */
public class AuditServiceImpl implements AuditServiceImplementor {

	private final ClassLoaderService classLoaderService;
	private final Map<Transaction, AuditProcess> auditProcesses;
	private AuditMetadata auditMetadata;

	public AuditServiceImpl(ServiceRegistryImplementor serviceRegistry) {
		this.classLoaderService = serviceRegistry.getService( ClassLoaderService.class );
		this.auditProcesses = new ConcurrentHashMap<>();
	}

	@Override
	public void initialize(AuditMetadata auditMetadata) {
		this.auditMetadata = auditMetadata;
	}

	@Override
	public AuditServiceOptions getOptions() {
		return auditMetadata.getAuditMetadataBuildingOptions();
	}

	@Override
	public EntitiesConfigurations getEntityBindings() {
		return auditMetadata.getEntitiesConfigurations();
	}

	@Override
	public AuditProcess getAuditProcess(EventSource session) {
		return getOrCreateAuditProcess( session );
	}

	@Override
	public RevisionInfoNumberReader getRevisionInfoNumberReader() {
		return auditMetadata.getRevisionInfoNumberReader();
	}

	@Override
	public RevisionInfoQueryCreator getRevisionInfoQueryCreator() {
		return auditMetadata.getRevisionInfoQueryCreator();
	}

	@Override
	public ModifiedEntityNamesReader getModifiedEntityNamesReader() {
		return auditMetadata.getModifiedEntityNamesReader();
	}

	@Override
	public String getAuditEntityName(String entityName) {
		return auditMetadata.getAuditMetadataBuildingOptions().getAuditEntityName( entityName );
	}

	@Override
	public String getRevisionPropertyPath(String propertyName) {
		return auditMetadata.getAuditMetadataBuildingOptions().getRevisionPropertyPath( propertyName );
	}

	@Override
	public ClassLoaderService getClassLoaderService() {
		return classLoaderService;
	}

	@Override
	public AuditMetadata getMetadata() {
		return auditMetadata;
	}

	/**
	 * Gets an existing or creates a new {@link AuditProcess}.
	 *
	 * @param session the session to create or locate an audit process association for.
	 * @return the AuditProcess.
	 */
	private AuditProcess getOrCreateAuditProcess(EventSource session) {
		final Transaction transaction = session.accessTransaction();
		AuditProcess auditProcess = auditProcesses.get( transaction );
		if ( auditProcess == null ) {
			auditProcess = createAuditProcess( session, transaction );
			auditProcesses.put( transaction, auditProcess );
		}
		return auditProcess;
	}

	/**
	 * Creates a new {@link AuditProcess}.
	 *
	 * @param session the session to associate with the audit process.
	 * @param transaction the transaction to associate with the audit process.
	 * @return the created AuditProcess.
	 */
	private AuditProcess createAuditProcess(EventSource session, Transaction transaction) {
		final AuditProcess auditProcess = new AuditProcess( auditMetadata.getRevisionInfoGenerator(), session );

		session.getActionQueue().registerProcess(
				sessionImplementor -> {
					final AuditProcess auditProcess1 = auditProcesses.get( transaction );
					if ( auditProcess1 != null ) {
						auditProcess1.doBeforeTransactionCompletion( sessionImplementor );
					}
				}
		);

		session.getActionQueue().registerProcess(
				( success, sessionImplementor ) -> {
					auditProcesses.remove( transaction );
				}
		);

		return auditProcess;
	}
}

package org.hibernate.envers.event.spi;

import org.jboss.jandex.DotName;

import org.hibernate.envers.AuditJoinTable;
import org.hibernate.envers.AuditMappedBy;
import org.hibernate.envers.AuditOverride;
import org.hibernate.envers.AuditOverrides;
import org.hibernate.envers.AuditTable;
import org.hibernate.envers.Audited;
import org.hibernate.envers.ModifiedEntityNames;
import org.hibernate.envers.NotAudited;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;
import org.hibernate.envers.SecondaryAuditTable;
import org.hibernate.envers.SecondaryAuditTables;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public interface EnversDotNames {
	DotName AUDITED = DotName.createSimple( Audited.class.getName() );
	DotName AUDIT_JOIN_TABLE = DotName.createSimple( AuditJoinTable.class.getName() );
	DotName AUDIT_MAPPED_BY = DotName.createSimple( AuditMappedBy.class.getName() );
	DotName AUDIT_OVERRIDE = DotName.createSimple( AuditOverride.class.getName() );
	DotName AUDIT_OVERRIDES = DotName.createSimple( AuditOverrides.class.getName() );
	DotName AUDIT_TABLE = DotName.createSimple( AuditTable.class.getName() );
	DotName MODIFIED_ENTITY_NAMES = DotName.createSimple( ModifiedEntityNames.class.getName() );
	DotName NOT_AUDITED = DotName.createSimple( NotAudited.class.getName() );
	DotName REVISION_ENTITY = DotName.createSimple( RevisionEntity.class.getName() );
	DotName REVISION_NUMBER = DotName.createSimple( RevisionNumber.class.getName() );
	DotName REVISION_TIMESTAMP = DotName.createSimple( RevisionTimestamp.class.getName() );
	DotName SECONDARY_AUDIT_TABLE = DotName.createSimple( SecondaryAuditTable.class.getName() );
	DotName SECONDARY_AUDIT_TABLES = DotName.createSimple( SecondaryAuditTables.class.getName() );
}

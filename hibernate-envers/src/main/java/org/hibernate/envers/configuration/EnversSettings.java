/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.configuration;

import org.hibernate.Internal;

/**
 * Configuration property names.
 *
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 * @author Chris Cranford
 */
public interface EnversSettings {
	/**
	 * Triggers revision generation when not-owned relation field changes. Defaults to {@code true}.
	 */
	String REVISION_ON_COLLECTION_CHANGE = "org.hibernate.envers.revision_on_collection_change";

	/**
	 * Treats optimistic locking properties as unversioned. Defaults to {@code true}.
	 */
	String DO_NOT_AUDIT_OPTIMISTIC_LOCKING_FIELD = "org.hibernate.envers.do_not_audit_optimistic_locking_field";

	/**
	 * Indicates whether entity data should be stored during removal. Defaults to {@code false}.
	 */
	String STORE_DATA_AT_DELETE = "org.hibernate.envers.store_data_at_delete";

	/**
	 * Default name of the schema containing audit tables.
	 */
	String DEFAULT_SCHEMA = "org.hibernate.envers.default_schema";

	/**
	 * Default name of the catalog containing audit tables.
	 */
	String DEFAULT_CATALOG = "org.hibernate.envers.default_catalog";

	/**
	 * Track entity names that have been changed during each revision. Defaults to {@code false}.
	 */
	String TRACK_ENTITIES_CHANGED_IN_REVISION = "org.hibernate.envers.track_entities_changed_in_revision";

	/**
	 * Use revision entity with native identifier generator. Defaults to {@code true} for backward compatibility.
	 */
	String USE_REVISION_ENTITY_WITH_NATIVE_ID = "org.hibernate.envers.use_revision_entity_with_native_id";

	/**
	 * Globally activates modified properties flag feature. Defaults to {@code false}.
	 */
	String GLOBAL_WITH_MODIFIED_FLAG = "org.hibernate.envers.global_with_modified_flag";

	/**
	 * Suffix of modified flag columns. Defaults to {@literal _MOD}.
	 */
	String MODIFIED_FLAG_SUFFIX = "org.hibernate.envers.modified_flag_suffix";

	/**
	 * Fully qualified class name of user defined revision listener.
	 */
	String REVISION_LISTENER = "org.hibernate.envers.revision_listener";

	/**
	 * Audit table prefix. Empty by default.
	 */
	String AUDIT_TABLE_PREFIX = "org.hibernate.envers.audit_table_prefix";

	/**
	 * Audit table suffix. Defaults to {@literal _AUD}.
	 */
	String AUDIT_TABLE_SUFFIX = "org.hibernate.envers.audit_table_suffix";

	/**
	 * Audit strategy. Defaults to {@link org.hibernate.envers.strategy.DefaultAuditStrategy}.
	 */
	String AUDIT_STRATEGY = "org.hibernate.envers.audit_strategy";

	/**
	 * Revision field name. Defaults to {@literal REV}.
	 */
	String REVISION_FIELD_NAME = "org.hibernate.envers.revision_field_name";

	/**
	 * Revision type field name. Defaults to {@literal REVTYPE}.
	 */
	String REVISION_TYPE_FIELD_NAME = "org.hibernate.envers.revision_type_field_name";

	/**
	 * Original id property name name. Defaults to {@literal originalId}.
	 */
	String ORIGINAL_ID_PROP_NAME = "org.hibernate.envers.original_id_prop_name";

	/**
	 * Column name that will hold the end revision number in audit entities. Defaults to {@literal REVEND}.
	 */
	String AUDIT_STRATEGY_VALIDITY_END_REV_FIELD_NAME = "org.hibernate.envers.audit_strategy_validity_end_rev_field_name";

	/**
	 * Store the timestamp of the end revision, until which the data was valid,
	 * in addition to the end revision itself. Defaults to {@code false}.
	 */
	String AUDIT_STRATEGY_VALIDITY_STORE_REVEND_TIMESTAMP = "org.hibernate.envers.audit_strategy_validity_store_revend_timestamp";

	/**
	 * Column name of the timestamp of the end revision until which the data was valid.
	 * Defaults to {@literal REVEND_TSTMP}.
	 */
	String AUDIT_STRATEGY_VALIDITY_REVEND_TIMESTAMP_FIELD_NAME = "org.hibernate.envers.audit_strategy_validity_revend_timestamp_field_name";

	/**
	 * Determines whether the timestamp of the end revision is stored as a numeric data type.
	 * Defaults to {@literal false}.
	 *
	 * @since 6.0
	 */
	String AUDIT_STRATEGY_VALIDITY_REVEND_TIMESTAMP_NUMERIC = "org.hibernate.envers.audit_strategy_validity_revend_timestamp_numeric";

	/**
	 * Whether to use legacy validity audit strategy revision end timestamp behavior where the field is not
	 * included as part of the joined entity inheritance subclass audit tables.
	 *
	 * Defaults to {@code true}.
	 *
	 * @since 6.0
	 */
	String AUDIT_STRATEGY_VALIDITY_REVEND_TIMESTAMP_LEGACY_PLACEMENT = "org.hibernate.envers.audit_strategy_validity_revend_timestamp_legacy_placement";

	/**
	 * Name of column used for storing ordinal of the change in sets of embeddable elements. Defaults to {@literal SETORDINAL}.
	 */
	String EMBEDDABLE_SET_ORDINAL_FIELD_NAME = "org.hibernate.envers.embeddable_set_ordinal_field_name";

	/**
	 * Guarantees proper validity audit strategy behavior when application reuses identifiers of deleted entities.
	 * Exactly one row with {@code null} end date exists for each identifier.
	 */
	String ALLOW_IDENTIFIER_REUSE = "org.hibernate.envers.allow_identifier_reuse";

	/**
	 * Forces {@code AuditReader#find} implementations that accept a revision-number argument to perform an exact
	 * match against the supplied revision number rather than potentially returning hits that are less-than or
	 * equal-to the supplied revision number.
	 *
	 * This option is meant to maintain backward compatibility while attempting to correct a bug in behavior without
	 * impacting existing users who may use the current behavior.
	 *
	 * Defaults to {@literal false}.
	 *
	 * @since 5.4.4
	 */
	String FIND_BY_REVISION_EXACT_MATCH = "org.hibernate.envers.find_by_revision_exact_match";

	/**
	 * Specifies the {@link org.hibernate.envers.boot.spi.ModifiedColumnNamingStrategy} to use
	 *
	 * Defaults to {@link org.hibernate.envers.boot.internal.LegacyModifiedColumnNamingStrategy}.
	 *
	 * @since 5.4.7
	 */
	String MODIFIED_COLUMN_NAMING_STRATEGY = "org.hibernate.envers.modified_column_naming_strategy";

	/**
	 * Deletion of a revision entity will cause a foreign key constraint database error when at least one
	 * audit record exists for that revision. By enabling this feature, deletion of the revision entity
	 * will also force all audit records associated to that revision to be deleted via cascade.
	 *
	 * Defaults to {@literal false}.
	 *
	 * @since 4.3.0
	 */
	String CASCADE_DELETE_REVISION = "org.hibernate.envers.cascade_delete_revision";

	/**
	 * Globally defines whether legacy relation not-found behavior should be used or not.
	 * Defaults to {@code true}.
	 *
	 * By specifying {@code true}, any {@code EntityNotFoundException} will be thrown unless the containing
	 * class or property explicitly specifies that use case to be ignored.  Conversely, when specifying the
	 * value {@code false}, the inverse applies and requires explicitly specifying the use case as error so
	 * that the exception is thrown.
	 */
	String GLOBAL_RELATION_NOT_FOUND_LEGACY_FLAG = "org.hibernate.envers.global_relation_not_found_legacy_flag";

	/**
	 * Whether to apply a nocache configuration for the revision sequence.
	 * This is mostly interesting for testing.
	 */
	@Internal
	String REVISION_SEQUENCE_NOCACHE = "org.hibernate.envers.revision_sequence_nocache";
}

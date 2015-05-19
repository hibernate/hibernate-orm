/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.configuration;

/**
 * Configuration property names.
 *
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public interface EnversSettings {
	/**
	 * Triggers revision generation when not-owned relation field changes. Defaults to {@code true}.
	 */
	public static final String REVISION_ON_COLLECTION_CHANGE = "org.hibernate.envers.revision_on_collection_change";

	/**
	 * Treats optimistic locking properties as unversioned. Defaults to {@code true}.
	 */
	public static final String DO_NOT_AUDIT_OPTIMISTIC_LOCKING_FIELD = "org.hibernate.envers.do_not_audit_optimistic_locking_field";

	/**
	 * Indicates whether entity data should be stored during removal. Defaults to {@code false}.
	 */
	public static final String STORE_DATA_AT_DELETE = "org.hibernate.envers.store_data_at_delete";

	/**
	 * Default name of the schema containing audit tables.
	 */
	public static final String DEFAULT_SCHEMA = "org.hibernate.envers.default_schema";

	/**
	 * Default name of the catalog containing audit tables.
	 */
	public static final String DEFAULT_CATALOG = "org.hibernate.envers.default_catalog";

	/**
	 * Track entity names that have been changed during each revision. Defaults to {@code false}.
	 */
	public static final String TRACK_ENTITIES_CHANGED_IN_REVISION = "org.hibernate.envers.track_entities_changed_in_revision";

	/**
	 * Use revision entity with native identifier generator. Defaults to {@code true} for backward compatibility.
	 */
	public static final String USE_REVISION_ENTITY_WITH_NATIVE_ID = "org.hibernate.envers.use_revision_entity_with_native_id";

	/**
	 * Globally activates modified properties flag feature. Defaults to {@code false}.
	 */
	public static final String GLOBAL_WITH_MODIFIED_FLAG = "org.hibernate.envers.global_with_modified_flag";

	/**
	 * Suffix of modified flag columns. Defaults to {@literal _MOD}.
	 */
	public static final String MODIFIED_FLAG_SUFFIX = "org.hibernate.envers.modified_flag_suffix";

	/**
	 * Fully qualified class name of user defined revision listener.
	 */
	public static final String REVISION_LISTENER = "org.hibernate.envers.revision_listener";

	/**
	 * Audit table prefix. Empty by default.
	 */
	public static final String AUDIT_TABLE_PREFIX = "org.hibernate.envers.audit_table_prefix";

	/**
	 * Audit table suffix. Defaults to {@literal _AUD}.
	 */
	public static final String AUDIT_TABLE_SUFFIX = "org.hibernate.envers.audit_table_suffix";

	/**
	 * Audit strategy. Defaults to {@link org.hibernate.envers.strategy.DefaultAuditStrategy}.
	 */
	public static final String AUDIT_STRATEGY = "org.hibernate.envers.audit_strategy";

	/**
	 * Revision field name. Defaults to {@literal REV}.
	 */
	public static final String REVISION_FIELD_NAME = "org.hibernate.envers.revision_field_name";

	/**
	 * Revision type field name. Defaults to {@literal REVTYPE}.
	 */
	public static final String REVISION_TYPE_FIELD_NAME = "org.hibernate.envers.revision_type_field_name";

	/**
	 * Column name that will hold the end revision number in audit entities. Defaults to {@literal REVEND}.
	 */
	public static final String AUDIT_STRATEGY_VALIDITY_END_REV_FIELD_NAME = "org.hibernate.envers.audit_strategy_validity_end_rev_field_name";

	/**
	 * Store the timestamp of the end revision, until which the data was valid,
	 * in addition to the end revision itself. Defaults to {@code false}.
	 */
	public static final String AUDIT_STRATEGY_VALIDITY_STORE_REVEND_TIMESTAMP = "org.hibernate.envers.audit_strategy_validity_store_revend_timestamp";

	/**
	 * Column name of the timestamp of the end revision until which the data was valid.
	 * Defaults to {@literal REVEND_TSTMP}.
	 */
	public static final String AUDIT_STRATEGY_VALIDITY_REVEND_TIMESTAMP_FIELD_NAME = "org.hibernate.envers.audit_strategy_validity_revend_timestamp_field_name";

	/**
	 * Name of column used for storing ordinal of the change in sets of embeddable elements. Defaults to {@literal SETORDINAL}.
	 */
	public static final String EMBEDDABLE_SET_ORDINAL_FIELD_NAME = "org.hibernate.envers.embeddable_set_ordinal_field_name";

	/**
	 * Guarantees proper validity audit strategy behavior when application reuses identifiers of deleted entities.
	 * Exactly one row with {@code null} end date exists for each identifier.
	 */
	public static final String ALLOW_IDENTIFIER_REUSE = "org.hibernate.envers.allow_identifier_reuse";
}

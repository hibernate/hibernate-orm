/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.binding.annotations.embedded;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.orm.test.util.SchemaUtil;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Focused coverage for component source member/path handling.
 */
public class ComponentSourceCleanupTest {
	@Test
	@DomainModel(annotatedClasses = ComponentPathEntity.class)
	@SessionFactory
	@ServiceRegistry(settings = @Setting(
			name = AvailableSettings.IMPLICIT_NAMING_STRATEGY,
			value = "component-path"
	))
	void componentSourceResolvesInheritedMembersOverridesAndFullNamingPath(SessionFactoryScope scope) {
		assertThat( SchemaUtil.isColumnPresent(
				"component_source_cleanup_entity",
				"component_embeddableCode",
				scope.getMetadataImplementor()
		) ).isTrue();
		assertThat( SchemaUtil.isColumnPresent(
				"component_source_cleanup_entity",
				"component_mappedCode",
				scope.getMetadataImplementor()
		) ).isTrue();
		assertThat( SchemaUtil.isColumnPresent(
				"component_source_cleanup_entity",
				"member_detail_value",
				scope.getMetadataImplementor()
		) ).isTrue();
		assertThat( SchemaUtil.isColumnPresent(
				"component_source_cleanup_entity",
				"entity_other_detail_value",
				scope.getMetadataImplementor()
		) ).isTrue();
		assertThat( SchemaUtil.isColumnPresent(
				"component_source_cleanup_entity",
				"base_detail_value",
				scope.getMetadataImplementor()
		) ).isFalse();
		assertThat( SchemaUtil.isColumnPresent(
				"component_source_cleanup_entity",
				"base_other_detail_value",
				scope.getMetadataImplementor()
		) ).isFalse();
	}

	@Test
	@DomainModel(annotatedClasses = JpaNamingEntity.class)
	@SessionFactory
	@ServiceRegistry(settings = @Setting(
			name = AvailableSettings.IMPLICIT_NAMING_STRATEGY,
			value = "jpa"
	))
	void jpaImplicitNamingStillUsesTerminalComponentProperty(SessionFactoryScope scope) {
		assertThat( SchemaUtil.isColumnPresent(
				"jpa_component_source_cleanup_entity",
				"value",
				scope.getMetadataImplementor()
		) ).isTrue();
		assertThat( SchemaUtil.isColumnPresent(
				"jpa_component_source_cleanup_entity",
				"component_detail_value",
				scope.getMetadataImplementor()
		) ).isFalse();
	}

	@MappedSuperclass
	@AttributeOverride(name = "component.detail.value", column = @Column(name = "base_detail_value"))
	@AttributeOverride(name = "other.detail.value", column = @Column(name = "base_other_detail_value"))
	public abstract static class BaseComponentPathEntity {
		@Embedded
		@AttributeOverride(name = "detail.value", column = @Column(name = "member_detail_value"))
		private ComponentValue component;

		@Embedded
		private ComponentValue other;
	}

	@Entity(name = "ComponentPathEntity")
	@Table(name = "component_source_cleanup_entity")
	@AttributeOverride(name = "other.detail.value", column = @Column(name = "entity_other_detail_value"))
	public static class ComponentPathEntity extends BaseComponentPathEntity {
		@Id
		private Integer id;
	}

	@Entity(name = "JpaNamingEntity")
	@Table(name = "jpa_component_source_cleanup_entity")
	public static class JpaNamingEntity {
		@Id
		private Integer id;

		@Embedded
		private ComponentValue component;
	}

	@Embeddable
	public static class ComponentValue extends EmbeddableComponentBase {
		@Embedded
		private ComponentDetail detail;
	}

	@Embeddable
	public static class EmbeddableComponentBase extends MappedComponentBase {
		private String embeddableCode;
	}

	@MappedSuperclass
	public abstract static class MappedComponentBase {
		private String mappedCode;
	}

	@Embeddable
	public static class ComponentDetail {
		private String value;
	}
}

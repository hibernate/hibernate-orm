/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy.proxy.inlinedirtychecking;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;

import org.hibernate.annotations.DynamicUpdate;

import org.hibernate.testing.bytecode.enhancement.CustomEnhancementContext;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Test for HHH-20897: With bytecode-enhanced inline dirty checking and @SecondaryTable,
 * dirtying one field on the primary table and one on the secondary table in the same flush
 * can silently drop the primary-table field's UPDATE when the secondary-table field name
 * sorts alphabetically before the primary-table field name.
 *
 * @author Chikumar
 */
@DomainModel(
		annotatedClasses = {
				SecondaryTableDynamicUpdateTest.Widget.class,
				SecondaryTableDynamicUpdateTest.WidgetControl.class
		}
)
@SessionFactory
@BytecodeEnhanced
@CustomEnhancementContext({ NoDirtyCheckEnhancementContext.class, DirtyCheckEnhancementContext.class })
@JiraKey("HHH-20897")
public class SecondaryTableDynamicUpdateTest {

	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			// Widget: secondary field (aSecondaryField) sorts alphabetically before primary field (zPrimaryField)
			// This is the failing case without the fix
			final Widget widget = new Widget();
			widget.setId( 1L );
			widget.setZPrimaryField( "primary_original" );
			widget.setASecondaryField( "secondary_original" );
			session.persist( widget );

			// WidgetControl: primary field (aPrimaryField) sorts alphabetically before secondary field (zSecondaryField)
			// This is the control case that works even without the fix
			final WidgetControl control = new WidgetControl();
			control.setId( 2L );
			control.setAPrimaryField( "primary_original" );
			control.setZSecondaryField( "secondary_original" );
			session.persist( control );
		} );
	}

	@Test
	public void testSecondaryTableFieldSortingBeforePrimaryField(SessionFactoryScope scope) {
		// Test the failing case: secondary field (aSecondaryField) sorts before primary field (zPrimaryField)
		scope.inTransaction( session -> {
			final Widget widget = session.find( Widget.class, 1L );
			// Update both fields in the same transaction
			widget.setZPrimaryField( "primary_updated" );
			widget.setASecondaryField( "secondary_updated" );
		} );

		// Verify both fields were updated correctly
		scope.inTransaction( session -> {
			final Widget widget = session.find( Widget.class, 1L );
			assertThat( "Primary field should be updated", widget.getZPrimaryField(), is( "primary_updated" ) );
			assertThat( "Secondary field should be updated", widget.getASecondaryField(), is( "secondary_updated" ) );
		} );
	}

	@Test
	public void testPrimaryTableFieldSortingBeforeSecondaryField(SessionFactoryScope scope) {
		// Test the control case: primary field (aPrimaryField) sorts before secondary field (zSecondaryField)
		scope.inTransaction( session -> {
			final WidgetControl control = session.find( WidgetControl.class, 2L );
			// Update both fields in the same transaction
			control.setAPrimaryField( "primary_updated" );
			control.setZSecondaryField( "secondary_updated" );
		} );

		// Verify both fields were updated correctly
		scope.inTransaction( session -> {
			final WidgetControl control = session.find( WidgetControl.class, 2L );
			assertThat( "Primary field should be updated", control.getAPrimaryField(), is( "primary_updated" ) );
			assertThat( "Secondary field should be updated", control.getZSecondaryField(), is( "secondary_updated" ) );
		} );
	}

	@Test
	public void testOnlyPrimaryFieldUpdate(SessionFactoryScope scope) {
		// Test updating only the primary field
		scope.inTransaction( session -> {
			final Widget widget = session.find( Widget.class, 1L );
			widget.setZPrimaryField( "primary_only_updated" );
		} );

		scope.inTransaction( session -> {
			final Widget widget = session.find( Widget.class, 1L );
			assertThat( "Primary field should be updated", widget.getZPrimaryField(), is( "primary_only_updated" ) );
			assertThat( "Secondary field should remain unchanged", widget.getASecondaryField(), is( "secondary_original" ) );
		} );
	}

	@Test
	public void testOnlySecondaryFieldUpdate(SessionFactoryScope scope) {
		// Test updating only the secondary field
		scope.inTransaction( session -> {
			final Widget widget = session.find( Widget.class, 1L );
			widget.setASecondaryField( "secondary_only_updated" );
		} );

		scope.inTransaction( session -> {
			final Widget widget = session.find( Widget.class, 1L );
			assertThat( "Primary field should remain unchanged", widget.getZPrimaryField(), is( "primary_original" ) );
			assertThat( "Secondary field should be updated", widget.getASecondaryField(), is( "secondary_only_updated" ) );
		} );
	}

	@Test
	public void testMultipleUpdatesInSequence(SessionFactoryScope scope) {
		// Test multiple updates in sequence
		scope.inTransaction( session -> {
			final Widget widget = session.find( Widget.class, 1L );
			widget.setZPrimaryField( "primary_update_1" );
			widget.setASecondaryField( "secondary_update_1" );
		} );

		scope.inTransaction( session -> {
			final Widget widget = session.find( Widget.class, 1L );
			assertThat( widget.getZPrimaryField(), is( "primary_update_1" ) );
			assertThat( widget.getASecondaryField(), is( "secondary_update_1" ) );

			// Update again
			widget.setZPrimaryField( "primary_update_2" );
			widget.setASecondaryField( "secondary_update_2" );
		} );

		scope.inTransaction( session -> {
			final Widget widget = session.find( Widget.class, 1L );
			assertThat( widget.getZPrimaryField(), is( "primary_update_2" ) );
			assertThat( widget.getASecondaryField(), is( "secondary_update_2" ) );
		} );
	}

	/**
	 * Entity with @SecondaryTable where the secondary field name (aSecondaryField)
	 * sorts alphabetically before the primary field name (zPrimaryField).
	 * This is the case that triggers the bug without the fix.
	 */
	@Entity(name = "Widget")
	@Table(name = "widget")
	@SecondaryTable(name = "widget_secondary")
	@DynamicUpdate
	public static class Widget {
		@Id
		private Long id;

		// Primary table field - sorts alphabetically AFTER 'aSecondaryField'
		@Column(name = "z_primary_field")
		private String zPrimaryField;

		// Secondary table field - sorts alphabetically BEFORE 'zPrimaryField'
		@Column(name = "a_secondary_field", table = "widget_secondary")
		private String aSecondaryField;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getZPrimaryField() {
			return zPrimaryField;
		}

		public void setZPrimaryField(String zPrimaryField) {
			this.zPrimaryField = zPrimaryField;
		}

		public String getASecondaryField() {
			return aSecondaryField;
		}

		public void setASecondaryField(String aSecondaryField) {
			this.aSecondaryField = aSecondaryField;
		}
	}

	/**
	 * Control entity with @SecondaryTable where the primary field name (aPrimaryField)
	 * sorts alphabetically before the secondary field name (zSecondaryField).
	 * This case works correctly even without the fix.
	 */
	@Entity(name = "WidgetControl")
	@Table(name = "widget_control")
	@SecondaryTable(name = "widget_control_secondary")
	@DynamicUpdate
	public static class WidgetControl {
		@Id
		private Long id;

		// Primary table field - sorts alphabetically BEFORE 'zSecondaryField'
		@Column(name = "a_primary_field")
		private String aPrimaryField;

		// Secondary table field - sorts alphabetically AFTER 'aPrimaryField'
		@Column(name = "z_secondary_field", table = "widget_control_secondary")
		private String zSecondaryField;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getAPrimaryField() {
			return aPrimaryField;
		}

		public void setAPrimaryField(String aPrimaryField) {
			this.aPrimaryField = aPrimaryField;
		}

		public String getZSecondaryField() {
			return zSecondaryField;
		}

		public void setZSecondaryField(String zSecondaryField) {
			this.zSecondaryField = zSecondaryField;
		}
	}
}

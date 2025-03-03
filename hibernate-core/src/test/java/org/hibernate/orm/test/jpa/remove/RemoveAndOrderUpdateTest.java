/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.remove;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

@Jpa(
		annotatedClasses = {
				RemoveAndOrderUpdateTest.Form.class,
				RemoveAndOrderUpdateTest.FormInput.class,
				RemoveAndOrderUpdateTest.FormVersion.class,
				RemoveAndOrderUpdateTest.FormVersion.class,
		},
		integrationSettings = {
				@Setting(name = AvailableSettings.ORDER_UPDATES, value = "true")
		}
)
@JiraKey("HHH-16923")
public class RemoveAndOrderUpdateTest {

	@Test
	public void testRemove(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Form aForm = new Form();
					entityManager.persist( aForm );

					FormVersion formVersion = new FormVersion( aForm, 1 );
					entityManager.persist( formVersion );

					FormVersion formVersion2 = new FormVersion( aForm, 2 );
					entityManager.persist( formVersion2 );
				}
		);

		scope.inTransaction(
				entityManager -> {
					List<FormVersion> formVersions = entityManager.createQuery(
							"select fv from FormVersion fv",
							FormVersion.class
					).getResultList();

					formVersions.forEach(
							entityManager::remove
					);
				}
		);
	}

	@Entity(name = "Form")
	public static class Form {
		@Id
		@GeneratedValue
		protected Long id;
		protected String name;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

	}

	@Entity(name = "FormInput")
	public static class FormInput {
		@Id
		@GeneratedValue
		private Long id;

		@ManyToOne(optional = false, fetch = FetchType.EAGER)
		@JoinColumns(foreignKey = @ForeignKey(name = "FK_Input_FormVersion"), value = {
				@JoinColumn(name = "form_id", updatable = false),
				@JoinColumn(name = "versionNumber", updatable = false)
		})
		private FormVersion formVersion;

		public FormInput(FormVersion formVersion) {
			this.formVersion = formVersion;
		}

		public FormInput() {
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public FormVersion getFormVersion() {
			return formVersion;
		}

		public void setFormVersion(FormVersion formVersion) {
			this.formVersion = formVersion;
		}

	}

	@Entity(name = "FormVersion")
	public static class FormVersion {

		@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "formVersion")
		private List<FormInput> inputs;

		@EmbeddedId
		protected FormVersionId id;

		public FormVersionId getId() {
			return id;
		}

		public void setId(FormVersionId id) {
			this.id = id;
		}

		public FormVersion() {
			id = new FormVersionId();
		}

		public FormVersion(Form form, int version) {
			this();
			this.id.setForm( form );
			this.id.setVersionNumber( version );
		}

		public List<FormInput> getInputs() {
			return inputs;
		}

		public void setInputs(List<FormInput> inputs) {
			this.inputs = inputs;
		}

	}

	@Embeddable
	public static class FormVersionId implements Serializable {

		@ManyToOne
		@JoinColumn(name = "form_id")
		private Form form;

		private Integer versionNumber;

		public Form getForm() {
			return form;
		}

		public void setForm(Form form) {
			this.form = form;
		}

		public Integer getVersionNumber() {
			return versionNumber;
		}

		public void setVersionNumber(Integer versionNumber) {
			this.versionNumber = versionNumber;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( !( o instanceof FormVersionId ) ) {
				return false;
			}
			FormVersionId that = (FormVersionId) o;
			return form.getId().equals( that.form.getId() ) && versionNumber.equals( that.versionNumber );
		}

		@Override
		public int hashCode() {
			return Objects.hash( form, versionNumber );
		}
	}
}

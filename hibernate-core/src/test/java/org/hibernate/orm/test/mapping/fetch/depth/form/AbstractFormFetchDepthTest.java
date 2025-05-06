/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.fetch.depth.form;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Marco Belladelli
 */
@SessionFactory
@DomainModel( annotatedClasses = {
		AbstractFormFetchDepthTest.Form.class,
		AbstractFormFetchDepthTest.FormOption.class,
		AbstractFormFetchDepthTest.FormVersion.class,
		AbstractFormFetchDepthTest.FormVersionId.class,
		AbstractFormFetchDepthTest.StepConfiguration.class,
} )
@Jira( "https://hibernate.atlassian.net/browse/HHH-16871" )
public abstract class AbstractFormFetchDepthTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Form form = new Form( new FormInfo( "test_form" ) );
			session.persist( form );
			final FormVersion formVersion = new FormVersion( form, 1 );
			session.persist( formVersion );
			final StepConfiguration stepConfiguration = new StepConfiguration( 1L );
			final FormOption formOption = new FormOption( 2L, stepConfiguration, formVersion );
			stepConfiguration.getFormOptions().add( formOption );
			session.persist( stepConfiguration );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from FormOption" ).executeUpdate();
			session.createMutationQuery( "delete from FormVersion" ).executeUpdate();
			session.createMutationQuery( "delete from Form" ).executeUpdate();
			session.createMutationQuery( "delete from StepConfiguration" ).executeUpdate();
		} );
	}

	@Test
	public void formVersionLoadTest(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final StepConfiguration configuration = session.createQuery(
					"SELECT s FROM StepConfiguration s WHERE s.id = :id",
					StepConfiguration.class
			).setParameter( "id", 1L ).getSingleResult();
			assertNotNull( configuration.getFormOptions() );
			assertEquals( 1, configuration.getFormOptions().size() );
			configuration.getFormOptions().forEach( formOption -> {
				final FormVersion fv = formOption.getFormVersion();
				assertThat( fv ).isNotNull();
				assertThat( fv.getId().getForm() ).isNotNull();
				assertThat( fv.getId().getForm().getFormInfo().getName() ).isEqualTo( "test_form" );
			} );
		} );
	}

	@Test
	public void formOptionsLoadTest(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final List<FormOption> fos = session.createQuery(
					"from FormOption fo",
					FormOption.class
			).getResultList();
			assertEquals( 1, fos.size() );
			fos.forEach( formOption -> {
				final FormVersion fv = formOption.getFormVersion();
				assertThat( fv ).isNotNull();
				assertThat( fv.getId().getForm() ).isNotNull();
				assertThat( fv.getId().getForm().getFormInfo().getName() ).isEqualTo( "test_form" );
			} );
		} );
	}

	@Test
	public void singleFormOptionLoadTest(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final FormOption fo = session.createQuery(
					"from FormOption fo where fo.id = :id",
					FormOption.class
			).setParameter( "id", 2L ).getSingleResult();
			final FormVersion fv = fo.getFormVersion();
			assertThat( fv ).isNotNull();
			assertThat( fv.getId().getForm() ).isNotNull();
			assertThat( fv.getId().getForm().getFormInfo().getName() ).isEqualTo( "test_form" );
		} );
	}

	@Embeddable
	public static class FormInfo implements Serializable {
		private String name;

		public FormInfo() {
		}

		public FormInfo(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}

	@Entity( name = "Form" )
	public static class Form {
		@EmbeddedId
		private FormInfo formInfo;

		@OneToMany( mappedBy = "id.form" )
		private List<FormVersion> versions;

		public Form() {
		}

		public Form(FormInfo formInfo) {
			this.formInfo = formInfo;
		}

		public List<FormVersion> getVersions() {
			return versions;
		}

		public FormInfo getFormInfo() {
			return formInfo;
		}
	}

	@Entity( name = "FormOption" )
	public static class FormOption {
		@Id
		private Long id;

		@ManyToOne( optional = false )
		@JoinColumn( name = "stepConfiguration_id", foreignKey = @ForeignKey( name = "FK_FormOption_StepConfiguration" ) )
		private StepConfiguration configuration;

		@ManyToOne( optional = false )
		@JoinColumns( foreignKey = @ForeignKey( name = "FK_FormOption_FormVersion" ), value = {
				@JoinColumn( name = "form_id", updatable = false ),
				@JoinColumn( name = "versionNumber", updatable = false )
		} )
		private FormVersion formVersion;

		public FormOption() {
		}

		public FormOption(Long id, StepConfiguration configuration, FormVersion formVersion) {
			this.id = id;
			this.configuration = configuration;
			this.formVersion = formVersion;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public StepConfiguration getConfiguration() {
			return configuration;
		}

		public void setConfiguration(StepConfiguration configuration) {
			this.configuration = configuration;
		}

		public FormVersion getFormVersion() {
			return formVersion;
		}

		public void setFormVersion(FormVersion formVersion) {
			this.formVersion = formVersion;
		}
	}

	@Entity( name = "FormVersion" )
	public static class FormVersion {
		@EmbeddedId
		private FormVersionId id;

		public FormVersion() {
			id = new FormVersionId();
		}

		public FormVersion(Form form, int version) {
			this();
			this.id.setForm( form );
			this.id.setVersionNumber( version );
		}

		public FormVersionId getId() {
			return id;
		}
	}

	@Embeddable
	public static class FormVersionId implements Serializable {
		@ManyToOne
		@JoinColumn( name = "form_id" )
		private Form form;

		private Integer versionNumber;

		public Form getForm() {
			return form;
		}

		public void setForm(Form form) {
			this.form = form;
		}

		public void setVersionNumber(Integer versionNumber) {
			this.versionNumber = versionNumber;
		}
	}

	@Entity( name = "StepConfiguration" )
	public static class StepConfiguration {
		@Id
		private Long id;

		@OneToMany( cascade = CascadeType.ALL, mappedBy = "configuration", fetch = FetchType.EAGER )
		private Set<FormOption> formOptions = new HashSet<>();

		public StepConfiguration() {
		}

		public StepConfiguration(Long id) {
			this.id = id;
		}

		public Set<FormOption> getFormOptions() {
			return formOptions;
		}
	}
}

package org.hibernate.orm.test.where.annotations;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Hibernate;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.SqlFragmentAlias;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(annotatedClasses = { RestrictedJoinedInheritanceTest.Project.class,
		RestrictedJoinedInheritanceTest.Version.class, RestrictedJoinedInheritanceTest.SpecialVersion.class })
@SessionFactory
@JiraKey("HHH-12016")
class RestrictedJoinedInheritanceTest {
	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Test
	void superclassColumnRestriction(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Project project = new Project();
			project.id = 1L;
			session.persist( project );
			for ( long id = 1; id <= 2; id++ ) {
				final SpecialVersion version = new SpecialVersion();
				version.id = id;
				version.active = id == 1;
				version.published = true;
				version.project = project;
				session.persist( version );
			}
		} );
		for ( boolean fetch : new boolean[] { false, true } ) {
			scope.inTransaction( session -> {
				session.enableFilter( "activeVersion" ).setParameter( "active", true );
				final Project project = fetch
						? session.createQuery( "from Project p left join fetch p.versions left join fetch p.filteredVersions", Project.class )
								.getSingleResult()
						: session.find( Project.class, 1L );
				Hibernate.initialize( project.versions );
				assertThat( project.versions ).extracting( v -> v.id ).containsExactly( 1L );
				assertThat( project.filteredVersions ).extracting( v -> v.id ).containsExactly( 1L );
			} );
		}
	}

	@Entity(name = "Project")
	@Table(name = "restriction_project")
	static class Project {
		@Id Long id;
		@OneToMany(mappedBy = "project")
		@SQLRestriction("ACTIVE = true and `published version` = true")
		List<SpecialVersion> versions = new ArrayList<>();
		@OneToMany(mappedBy = "project")
		@Filter(name = "activeVersion", condition = "{v}.active = :active", deduceAliasInjectionPoints = false,
				aliases = @SqlFragmentAlias(alias = "v", table = "restriction_version"))
		Set<SpecialVersion> filteredVersions = new HashSet<>();
	}
	@Entity(name = "Version")
	@Table(name = "restriction_version")
	@Inheritance(strategy = InheritanceType.JOINED)
	@FilterDef(name = "activeVersion", parameters = @ParamDef(name = "active", type = Boolean.class))
	static class Version {
		@Id Long id;
		boolean active;
		@Column(name = "`published version`")
		boolean published;
	}
	@Entity(name = "SpecialVersion")
	@Table(name = "restriction_special_version")
	static class SpecialVersion extends Version {
		@ManyToOne Project project;
	}
}

package org.hibernate.orm.test.boot.models.xml.attr;

import java.util.Set;

import org.hibernate.annotations.Changelog;
import org.hibernate.audit.ChangesetListener;
import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.boot.model.source.internal.annotations.AdditionalManagedResourcesImpl;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassDetailsRegistry;
import org.hibernate.models.spi.FieldDetails;
import org.hibernate.models.spi.ModelsContext;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.orm.test.boot.models.SourceModelTestHelper.createBuildingContext;

@JiraKey("HHH-20617")
@ServiceRegistry
public class ChangelogXmlAttributeTests {

	@Test
	@SuppressWarnings("JUnitMalformedDeclaration")
	void testChangelogFromXml(ServiceRegistryScope scope) {
		final StandardServiceRegistry serviceRegistry = scope.getRegistry();
		final ManagedResources managedResources = new AdditionalManagedResourcesImpl.Builder()
				.addXmlMappings( "mappings/models/attr/changelog/changelog.xml" )
				.build();

		final ModelsContext modelsContext = createBuildingContext( managedResources, serviceRegistry );
		final ClassDetailsRegistry classDetailsRegistry = modelsContext.getClassDetailsRegistry();

		final ClassDetails classDetails = classDetailsRegistry.getClassDetails( SimpleChangelog.class.getName() );

		// entity-level @Changelog
		final Changelog changelog = classDetails.getDirectAnnotationUsage( Changelog.class );
		assertThat( changelog )
				.as( "Entity with <changelog> should have @Changelog" )
				.isNotNull();
		assertThat( changelog.listener() ).isEqualTo( SimpleChangesetListener.class );

		// @Changelog.ChangesetId via <changeset-id>
		final FieldDetails idField = classDetails.findFieldByName( "id" );
		assertThat( idField.getDirectAnnotationUsage( Changelog.ChangesetId.class ) )
				.as( "Id with <changeset-id> should have @Changelog.ChangesetId" )
				.isNotNull();

		// @Changelog.Timestamp via <changelog-timestamp>
		final FieldDetails timestampField = classDetails.findFieldByName( "timestamp" );
		assertThat( timestampField.getDirectAnnotationUsage( Changelog.Timestamp.class ) )
				.as( "Basic with <changelog-timestamp> should have @Changelog.Timestamp" )
				.isNotNull();

		// @Changelog.ModifiedEntities via <changelog-modified-entities>
		final FieldDetails modifiedEntitiesField = classDetails.findFieldByName( "modifiedEntities" );
		assertThat( modifiedEntitiesField.getDirectAnnotationUsage( Changelog.ModifiedEntities.class ) )
				.as( "Collection with <changelog-modified-entities> should have @Changelog.ModifiedEntities" )
				.isNotNull();
	}

	public static class SimpleChangesetListener implements ChangesetListener {
		@Override
		public void newChangeset(Object changelog) {
		}
	}

	@SuppressWarnings("unused")
	@Entity(name = "SimpleChangelog")
	@Table(name = "SimpleChangelog")
	public static class SimpleChangelog {
		@Id
		@GeneratedValue
		private int id;
		private long timestamp;
		private Set<String> modifiedEntities;
	}
}

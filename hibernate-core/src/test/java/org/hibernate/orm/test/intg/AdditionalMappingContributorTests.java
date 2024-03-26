/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.intg;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.hibernate.boot.ResourceStreamLocator;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.spi.AdditionalMappingContributions;
import org.hibernate.boot.spi.AdditionalMappingContributor;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.models.internal.jdk.JdkClassDetails;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassDetailsRegistry;
import org.hibernate.models.spi.FieldDetails;
import org.hibernate.models.spi.MutableAnnotationUsage;
import org.hibernate.models.spi.MutableMemberDetails;

import org.hibernate.testing.orm.junit.BootstrapServiceRegistry;
import org.hibernate.testing.orm.junit.BootstrapServiceRegistry.JavaService;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 *
 * @implNote hibernate-envers is already a full testing of contributing a {@code hbm.xml}
 * document; so we skip that here until if/when we transition it to use a better approach
 */
@BootstrapServiceRegistry(
		javaServices = @JavaService(
				role = AdditionalMappingContributor.class,
				impl = AdditionalMappingContributorTests.AdditionalMappingContributorImpl.class
		)
)
@DomainModel( annotatedClasses = AdditionalMappingContributorTests.Entity1.class )
@SessionFactory
public class AdditionalMappingContributorTests {

	@Test
	void verifyClassContribution(DomainModelScope domainModelScope, SessionFactoryScope sessionFactoryScope) {
		final PersistentClass binding = domainModelScope.getDomainModel().getEntityBinding( Entity2.class.getName() );
		assertThat( binding ).isNotNull();
		assertThat( binding.getIdentifierProperty() ).isNotNull();
		assertThat( binding.getProperties() ).hasSize( 1 );

		sessionFactoryScope.inTransaction( (session) -> {
			final List<?> results = session.createSelectionQuery( "from Entity2" ).list();
			assertThat( results ).hasSize( 0 );
		} );
	}

	@Test
	void verifyClassDetailsContributions(DomainModelScope domainModelScope, SessionFactoryScope sessionFactoryScope) {
		{
			final PersistentClass binding = domainModelScope.getDomainModel().getEntityBinding( Entity4.class.getName() );
			assertThat( binding ).isNotNull();
			assertThat( binding.getIdentifierProperty() ).isNotNull();
			assertThat( binding.getProperties() ).hasSize( 1 );
		}

		{
			final PersistentClass binding = domainModelScope.getDomainModel().getEntityBinding( Entity5.class.getName() );
			assertThat( binding ).isNotNull();
			assertThat( binding.getIdentifierProperty() ).isNotNull();
			assertThat( binding.getProperties() ).hasSize( 1 );
		}

		sessionFactoryScope.inTransaction( (session) -> {
			final List<?> results4 = session.createSelectionQuery( "from Entity4" ).list();
			assertThat( results4 ).hasSize( 0 );

			final List<?> results5 = session.createSelectionQuery( "from ___Entity5___" ).list();
			assertThat( results5 ).hasSize( 0 );

		} );
	}

	@Test
	void verifyOrmXmlContribution(DomainModelScope domainModelScope, SessionFactoryScope sessionFactoryScope) {
		final PersistentClass binding = domainModelScope.getDomainModel().getEntityBinding( Entity3.class.getName() );
		assertThat( binding ).isNotNull();
		assertThat( binding.getIdentifierProperty() ).isNotNull();
		assertThat( binding.getProperties() ).hasSize( 1 );

		sessionFactoryScope.inTransaction( (session) -> {
			final List<?> results = session.createSelectionQuery( "from Entity3" ).list();
			assertThat( results ).hasSize( 0 );
		} );
	}

	@Entity( name = "Entity1" )
	@Table( name = "Entity1" )
	public static class Entity1 {
	    @Id
	    private Integer id;
	    @Basic
		private String name;

		private Entity1() {
			// for use by Hibernate
		}

		public Entity1(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}


	@Entity( name = "Entity2" )
	@Table( name = "Entity2" )
	public static class Entity2 {
	    @Id
	    private Integer id;
	    @Basic
		private String name;

		private Entity2() {
			// for use by Hibernate
		}

		public Entity2(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity( name = "Entity3" )
	@Table( name = "Entity3" )
	public static class Entity3 {
	    @Id
	    private Integer id;
	    @Basic
		private String name;

		private Entity3() {
			// for use by Hibernate
		}

		public Entity3(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity(name="Entity4")
	@Table(name="Entity4")
	public static class Entity4 {
		@Id
		private Integer id;
		private String name;
	}

	public static class Entity5 {
		private Integer id;
		private String name;
	}

	public static class AdditionalMappingContributorImpl implements AdditionalMappingContributor {
		@Override
		public void contribute(
				AdditionalMappingContributions contributions,
				InFlightMetadataCollector metadata,
				ResourceStreamLocator resourceStreamLocator,
				MetadataBuildingContext buildingContext) {
			contributions.contributeEntity( Entity2.class );

			final ClassDetailsRegistry classDetailsRegistry = buildingContext.getMetadataCollector()
					.getSourceModelBuildingContext()
					.getClassDetailsRegistry();

			final ClassDetails entity4Details = classDetailsRegistry.resolveClassDetails(
					Entity4.class.getName(),
					(name, modelBuildingContext) -> {
						assertThat( name ).isEqualTo( Entity4.class.getName() );
						assertThat( modelBuildingContext ).isSameAs( buildingContext.getMetadataCollector().getSourceModelBuildingContext() );
						return new JdkClassDetails( Entity4.class, modelBuildingContext );
					}
			);
			contributions.contributeManagedClass( entity4Details );

			final ClassDetails entity5Details = classDetailsRegistry.resolveClassDetails(
					Entity5.class.getName(),
					(name, modelBuildingContext) -> {
						assertThat( name ).isEqualTo( Entity5.class.getName() );
						assertThat( modelBuildingContext ).isSameAs( buildingContext.getMetadataCollector().getSourceModelBuildingContext() );
						final JdkClassDetails jdkClassDetails = new JdkClassDetails(
								Entity5.class,
								modelBuildingContext
						);

						final MutableAnnotationUsage<Entity> entityAnnotation = JpaAnnotations.ENTITY.createUsage(
								jdkClassDetails,
								modelBuildingContext
						);
						entityAnnotation.setAttributeValue( "name", "___Entity5___" );
						jdkClassDetails.addAnnotationUsage( entityAnnotation );

						final MutableMemberDetails idField = (MutableMemberDetails) jdkClassDetails.findFieldByName( "id" );
						idField.addAnnotationUsage( JpaAnnotations.ID.createUsage( idField, modelBuildingContext ) );

						return jdkClassDetails;
					}
			);
			contributions.contributeManagedClass( entity5Details );

			try ( final InputStream stream = resourceStreamLocator.locateResourceStream( "mappings/intg/contributed-mapping.xml" ) ) {
				contributions.contributeBinding( stream );
			}
			catch (IOException e) {
				throw new RuntimeException( e );
			}
		}
	}
}

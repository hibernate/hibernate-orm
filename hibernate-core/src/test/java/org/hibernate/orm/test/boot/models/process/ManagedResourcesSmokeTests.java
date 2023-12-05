/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.boot.models.process;

import java.util.Iterator;
import java.util.Set;

import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.internal.BootstrapContextImpl;
import org.hibernate.boot.internal.MetadataBuilderImpl;
import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.boot.model.process.spi.MetadataBuildingProcess;
import org.hibernate.boot.models.categorize.spi.AttributeMetadata;
import org.hibernate.boot.models.categorize.spi.BasicKeyMapping;
import org.hibernate.boot.models.categorize.spi.CategorizedDomainModel;
import org.hibernate.boot.models.categorize.spi.EntityHierarchy;
import org.hibernate.boot.models.categorize.spi.EntityTypeMetadata;
import org.hibernate.boot.models.categorize.spi.ManagedResourcesProcessor;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.FieldDetails;

import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.spi.PersistenceUnitInfo;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@linkplain MetadataBuildingProcess#prepare} produces a {@linkplain ManagedResources} which
 * represents the complete set of classes and xml mappings.
 * <p>
 * The known XML mappings combine explicit mappings, plus any discovered during scanning.
 * <p>
 * The known classes combine explicit values, plus any discovered during scanning.  It also
 * already excludes classes as per JPA's {@linkplain PersistenceUnitInfo#excludeUnlistedClasses()}
 * handling.
 * <p/>
 * The "known classes" + all classes named in "known XML mappings" represents the complete
 * set of "managed classes" (JPA's term) which Hibernate will process for annotations -
 * {@code @Entity}, {@code @Converter}, ...
 * <p/>
 * Let's see how using that with hibernate-models that might work...
 *
 * @author Steve Ebersole
 */
public class ManagedResourcesSmokeTests {
	@Test
	void testCategorization() {
		try (StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().build()) {
			final MetadataSources metadataSources = new MetadataSources( serviceRegistry ).addAnnotatedClass( Person.class );
			final MetadataBuilderImpl.MetadataBuildingOptionsImpl metadataBuildingOptions = new MetadataBuilderImpl.MetadataBuildingOptionsImpl( serviceRegistry );
			final BootstrapContextImpl bootstrapContext = new BootstrapContextImpl( serviceRegistry, metadataBuildingOptions );
			final ManagedResources managedResources = MetadataBuildingProcess.prepare( metadataSources, bootstrapContext );

			final CategorizedDomainModel categorizedDomainModel = ManagedResourcesProcessor.processManagedResources(
					managedResources,
					bootstrapContext
			);

			final Set<EntityHierarchy> entityHierarchies = categorizedDomainModel.getEntityHierarchies();
			assertThat( entityHierarchies ).hasSize( 1 );
			final EntityHierarchy entityHierarchy = entityHierarchies.iterator().next();
			final EntityTypeMetadata entityDescriptor = entityHierarchy.getRoot();
			final ClassDetails entityClassDetails = entityDescriptor.getClassDetails();
			assertThat( entityClassDetails.getClassName() ).isEqualTo( Person.class.getName() );
			assertThat( entityDescriptor.getAttributes() ).hasSize( 2 );

			final Iterator<AttributeMetadata> attributes = entityDescriptor.getAttributes().iterator();
			final AttributeMetadata firstAttribute = attributes.next();
			final AttributeMetadata secondAttribute = attributes.next();

			assertThat( firstAttribute.getMember() ).isInstanceOf( FieldDetails.class );
			assertThat( secondAttribute.getMember() ).isInstanceOf( FieldDetails.class );

			assertThat( entityHierarchy.getIdMapping() ).isInstanceOf( BasicKeyMapping.class );
			final BasicKeyMapping idMapping = (BasicKeyMapping) entityHierarchy.getIdMapping();
			final AttributeMetadata idAttribute = idMapping.getAttribute();
			assertThat( idAttribute.getName() ).isEqualTo( "id" );
			assertThat( idAttribute.getMember() ).isInstanceOf( FieldDetails.class );
		}
	}

	@Entity(name="Person")
	@Table(name="persons", comment = "We store stuff here")
	@FilterDef( name = "name", defaultCondition = "name = :name", parameters = @ParamDef( name = "name", type = String.class ) )
	public static class Person {
		@Id
		private Integer id;
		private String name;
	}
}

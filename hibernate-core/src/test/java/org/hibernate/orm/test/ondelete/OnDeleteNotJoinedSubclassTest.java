package org.hibernate.orm.test.ondelete;

import org.hibernate.boot.pipeline.internal.MetadataBuildingHelper;
import org.hibernate.boot.pipeline.internal.source.MappingSources;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;

import org.hibernate.AnnotationException;
import org.hibernate.annotations.OnDelete;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import static jakarta.persistence.InheritanceType.JOINED;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.annotations.OnDeleteAction.CASCADE;

@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey("HHH-21203")
@ServiceRegistry
public class OnDeleteNotJoinedSubclassTest {

	@Test
	void rootEntity(ServiceRegistryScope registryScope) {
		assertThatThrownBy( () -> MetadataBuildingHelper.buildMetadata( registryScope.getRegistry(), new MappingSources().addManagedClasses( AnnotatedRoot.class ) ) )
				.isInstanceOf( AnnotationException.class )
				.hasMessageContaining( AnnotatedRoot.class.getName() )
				.hasMessageContaining( "may not be annotated '@OnDelete'" );
	}

	@Test
	void singleTableSubclass(ServiceRegistryScope registryScope) {
		assertThatThrownBy( () -> MetadataBuildingHelper.buildMetadata( registryScope.getRegistry(), new MappingSources().addManagedClasses( SingleTableRoot.class, SingleTableChild.class ) ) )
				.isInstanceOf( AnnotationException.class )
				.hasMessageContaining( SingleTableChild.class.getName() )
				.hasMessageContaining( "may not be annotated '@OnDelete'" );
	}

	@Test
	void joinedSubclass(ServiceRegistryScope registryScope) {
		assertThatCode( () -> MetadataBuildingHelper.buildMetadata( registryScope.getRegistry(), new MappingSources().addManagedClasses( JoinedRoot.class, JoinedChild.class ) ) )
				.doesNotThrowAnyException();
	}

	@Entity(name = "AnnotatedRoot")
	@OnDelete(action = CASCADE)
	static class AnnotatedRoot {
		@Id
		long id;
	}

	@Entity(name = "SingleTableRoot")
	static class SingleTableRoot {
		@Id
		long id;
	}

	@Entity(name = "SingleTableChild")
	@OnDelete(action = CASCADE)
	static class SingleTableChild extends SingleTableRoot {
	}

	@Entity(name = "JoinedRoot")
	@Inheritance(strategy = JOINED)
	static class JoinedRoot {
		@Id
		long id;
	}

	@Entity(name = "JoinedChild")
	@OnDelete(action = CASCADE)
	static class JoinedChild extends JoinedRoot {
	}
}

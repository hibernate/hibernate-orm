package org.hibernate.orm.test.mapping.joinformula;

import java.io.Serializable;
import java.io.StringReader;
import javax.xml.transform.stream.StreamSource;

import org.hibernate.FetchNotFoundException;
import org.hibernate.annotations.JoinFormula;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.PhysicalNamingStrategySnakeCaseImpl;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.xsd.MappingXsdSupport;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.OneToOne;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/// Regression coverage for formula-based one-to-one associations.
///
/// The owner identifiers have two columns so these mappings exercise explicit
/// formula joins instead of shared-primary-key inference.
///
/// @author Steve Ebersole
@DomainModel(annotatedClasses = {
		FormulaOneToOneReviewTest.MissingOwner.class,
		FormulaOneToOneReviewTest.Target.class
})
@SessionFactory
public class FormulaOneToOneReviewTest {
	@Test
	public void testExplicitNotFoundExceptionIsHonored(SessionFactoryScope scope) {
		scope.inTransaction(session -> {
			MissingOwner owner = new MissingOwner();
			owner.id = 1L;
			owner.otherId = 2L;
			owner.targetId = 42L;
			session.persist(owner);
		});

		assertThrows(FetchNotFoundException.class, () -> scope.inTransaction(session ->
				session.find(MissingOwner.class, new MissingOwnerId(1L, 2L))));
	}

	@Test
	public void testReferencedLogicalColumnName() {
		var registry = new StandardServiceRegistryBuilder()
				.applySetting("hibernate.dialect", "org.hibernate.dialect.H2Dialect")
				.applySetting("hibernate.boot.allow_jdbc_metadata_access", false)
				.build();
		try {
			var metadata = new MetadataSources(registry)
					.addAnnotatedClass(NamedOwner.class)
					.addAnnotatedClass(NamedTarget.class)
					.getMetadataBuilder()
					.applyPhysicalNamingStrategy(new PhysicalNamingStrategySnakeCaseImpl())
					.build();
			assertNotNull(metadata.getEntityBinding(NamedOwner.class.getName()));
		}
		finally {
			StandardServiceRegistryBuilder.destroy(registry);
		}
	}

	@Test
	public void testExistingNonPrimaryKeyFormulaReference() {
		var registry = new StandardServiceRegistryBuilder()
				.applySetting("hibernate.dialect", "org.hibernate.dialect.H2Dialect")
				.applySetting("hibernate.boot.allow_jdbc_metadata_access", false)
				.build();
		try {
			new MetadataSources(registry)
					.addAnnotatedClass(CodeOwner.class)
					.addAnnotatedClass(CodeTarget.class)
					.buildMetadata();
		}
		finally {
			StandardServiceRegistryBuilder.destroy(registry);
		}
	}

	@Test
	public void testFormulaOneToOneXmlValidates() throws Exception {
		var descriptor = MappingXsdSupport._80;
		String xml = """
				<entity-mappings xmlns="%s" version="%s">
					<entity class="Owner">
						<attributes>
							<id name="id"/>
							<one-to-one name="target" target-entity="Target" optional="true">
								<join-formula>targetId</join-formula>
							</one-to-one>
						</attributes>
					</entity>
					<entity class="Target">
						<attributes><id name="id"/></attributes>
					</entity>
				</entity-mappings>
				""".formatted(descriptor.getNamespaceUri(), descriptor.getVersion());

		// The existing property-ref form is a control for the rest of the XML shape.
		String withPropertyRef = xml.replace("</one-to-one>",
				"<property-ref name=\"id\"/></one-to-one>");
		descriptor.getSchema().newValidator().validate(new StreamSource(new StringReader(withPropertyRef)));
		descriptor.getSchema().newValidator().validate(new StreamSource(new StringReader(xml)));
	}

	@Entity(name = "MissingOwner")
	@IdClass(MissingOwnerId.class)
	public static class MissingOwner {
		@Id
		Long id;
		@Id
		Long otherId;
		Long targetId;

		@OneToOne
		@NotFound(action = NotFoundAction.EXCEPTION)
		@JoinFormula(value = "targetId", referencedColumnName = "id")
		Target target;
	}

	public record MissingOwnerId(Long id, Long otherId) implements Serializable {}

	public record NamedOwnerId(Long id, Long otherId) implements Serializable {}

	@Entity(name = "Target")
	public static class Target {
		@Id
		Long id;
	}

	@Entity(name = "NamedOwner")
	@IdClass(NamedOwnerId.class)
	public static class NamedOwner {
		@Id
		Long id;
		@Id
		Long otherId;
		Long targetFk;

		@OneToOne
		@JoinFormula(value = "target_fk", referencedColumnName = "targetId")
		NamedTarget target;
	}

	@Entity(name = "NamedTarget")
	public static class NamedTarget {
		@Id
		@Column(name = "targetId")
		Long id;
	}

	@Entity(name = "CodeOwner")
	@IdClass(NamedOwnerId.class)
	public static class CodeOwner {
		@Id
		Long id;
		@Id
		Long otherId;
		String targetCode;

		@OneToOne
		@JoinFormula(value = "targetCode", referencedColumnName = "code")
		CodeTarget target;
	}

	@Entity(name = "CodeTarget")
	public static class CodeTarget {
		@Id
		Long id;
		@Column(unique = true)
		String code;
	}
}

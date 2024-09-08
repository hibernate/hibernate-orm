package org.hibernate.orm.test.annotations.basic;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.MappingException;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@JiraKey("HHH-17739")
@Jpa(annotatedClasses = ListOfByteArrayTest.Broken.class)
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsStandardArrays.class)
public class ListOfByteArrayTest {
	@Test void test(EntityManagerFactoryScope scope) {
		try {
			scope.getEntityManagerFactory();
			fail();
		}
		catch (MappingException e) {
			assertTrue( e.getMessage().contains("binaryList") );
		}
	}
	@Entity
	static class Broken {
		@Id long id;
		List<byte[]> binaryList; // this is not supported
	}
}

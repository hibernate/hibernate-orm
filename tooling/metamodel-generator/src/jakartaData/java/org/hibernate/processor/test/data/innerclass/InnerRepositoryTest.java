/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.innerclass;

import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;
import jakarta.data.repository.DataRepository;
import jakarta.data.repository.Find;
import jakarta.data.repository.Repository;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.Test;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.processor.test.util.TestUtil.getMetaModelSourceAsString;

public class InnerRepositoryTest extends CompilationTest {

	@Test
	@WithClasses({Thing.class, ThingRepo.class})
	public void test() {
		System.out.println( getMetaModelSourceAsString( ThingRepo.class ) );
		assertMetamodelClassGeneratedFor( ThingRepo.class );
	}

	@Entity
	public static class Thing {
		@Id
		Long id;

		String data;
	}

	@Repository
	public interface ThingRepo extends DataRepository<Thing, Long> {
		@Find
		Page<Thing> thing(PageRequest pageRequest);
	}
}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.jaxb.mapping;


import org.hibernate.boot.archive.internal.RepeatableInputStreamAccess;
import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.SourceType;
import org.hibernate.boot.jaxb.internal.MappingBinder;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.fail;


/**
 * @author Jan Schatteman
 */
public class OrmXmlValidationTest {

	@Test
	@ServiceRegistry
	public void testValidatingOK_Hbm(ServiceRegistryScope scope) {
		scope.withService( ClassLoaderService.class,
				(cls) -> {
					final String resourceName = "xml/jaxb/mapping/basic/hbm.xml";
					try (InputStream inputStream = cls.locateResourceStream( resourceName )) {
						final MappingBinder mappingBinder = new MappingBinder( cls, MappingBinder.DEFAULT_VALIDATING );
						mappingBinder.bind(
								new RepeatableInputStreamAccess( resourceName, inputStream ),
								new Origin( SourceType.RESOURCE, resourceName )
						);
					}
					catch (Exception e) {
						fail();
					}
				}
		);
	}

	@Test
	@ServiceRegistry
	public void testValidatingOK_Orm(ServiceRegistryScope scope) {
		scope.withService( ClassLoaderService.class,
				(cls) -> {
					final String resourceName = "xml/jaxb/mapping/validation/orm.xml";
					try (InputStream inputStream = cls.locateResourceStream( resourceName )) {
						final MappingBinder mappingBinder = new MappingBinder( cls, MappingBinder.DEFAULT_VALIDATING );
						mappingBinder.bind(
								new RepeatableInputStreamAccess( resourceName, inputStream ),
								new Origin( SourceType.RESOURCE, resourceName )
						);
					}
					catch (Exception e) {
						fail();
					}
				}
		);
	}

	@Test
	@ServiceRegistry
	public void testStrictValidatingOK_Orm(ServiceRegistryScope scope) {
		scope.withService( ClassLoaderService.class,
				(cls) -> {
					final String resourceName = "xml/jaxb/mapping/validation/orm.xml";
					try (InputStream inputStream = cls.locateResourceStream( resourceName )) {
						final MappingBinder mappingBinder = new MappingBinder( cls, MappingBinder.STRICT_VALIDATING );
						mappingBinder.bind(
								new RepeatableInputStreamAccess( resourceName, inputStream ),
								new Origin( SourceType.RESOURCE, resourceName )
						);
					}
					catch (Exception e) {
						fail();
					}
				}
		);
	}

	@Test
	@ServiceRegistry
	public void testStrictValidatingKO_Orm(ServiceRegistryScope scope) {
		scope.withService( ClassLoaderService.class,
				(cls) -> {
					final String resourceName = "xml/jaxb/mapping/validation/orm-specific_attrib.xml";
					try ( InputStream inputStream = cls.locateResourceStream( resourceName ) ) {
						final MappingBinder mappingBinder = new MappingBinder( cls, MappingBinder.STRICT_VALIDATING );
						mappingBinder.bind(
								new RepeatableInputStreamAccess( resourceName, inputStream ),
								new Origin( SourceType.RESOURCE, resourceName )
						);
						fail();
					}
					catch (Exception e) {
						// Nothing to do
					}
				}
		);
	}

}

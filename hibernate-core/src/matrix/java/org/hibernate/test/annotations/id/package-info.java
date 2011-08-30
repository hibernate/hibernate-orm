//$Id$
/**
 * Test package for metatata facilities
 * It contains an example of package level metadata
 */
@org.hibernate.annotations.GenericGenerator(name = "system-uuid", strategy = "uuid")
@org.hibernate.annotations.GenericGenerators(
		@org.hibernate.annotations.GenericGenerator(name = "system-uuid-2", strategy = "uuid")
)
package org.hibernate.test.annotations.id;



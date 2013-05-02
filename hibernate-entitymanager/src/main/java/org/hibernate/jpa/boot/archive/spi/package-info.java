/**
 * Defines the SPI for support of "scanning" of "archives".
 * <p/>
 * Scanning might mean:<ul>
 *     <li>searching for classes/packages that define certain interfaces</li>
 *     <li>locating named resources</li>
 * </ul>
 * And "archive" might mean:<ul>
 *     <li>a {@code .jar} file</li>
 *     <li>an exploded directory</li>
 *     <li>an OSGi bundle</li>
 *     <li>etc</li>
 * </ul>
 */
package org.hibernate.jpa.boot.archive.spi;

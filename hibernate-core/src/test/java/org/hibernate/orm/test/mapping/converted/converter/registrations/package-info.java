/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */

/**
 * Demonstrates the use of {@link org.hibernate.annotations.ConverterRegistration}.  See<ul>
 *     <li>
 *         {@link org.hibernate.orm.test.mapping.converted.converter.registrations.TheEntity}
 *         for example of using registrations to ENABLE auto-apply (regardless of what the
 *         converter defines)
 *     </li>
 *     <li>
 *         {@link org.hibernate.orm.test.mapping.converted.converter.registrations.TheEntity2}
 *         for example of using registrations to DISABLE auto-apply (regardless of what the
 *         converter defines)
 *     </li>
 *     <li>
 *         {@link org.hibernate.orm.test.mapping.converted.converter.registrations.AnotherEntity}
 *         for example of registering an auto-apply converter based on a type other than what the
 *         converter advertises it converts
 *     </li>
 * </ul>
 * <p>
 * Additionally, see the following tests for various "edge case" scenarios:<ul>
 *     <li>{@link org.hibernate.orm.test.mapping.converted.converter.registrations.MatchingDuplicateRegistrationTests}</li>
 *     <li>{@link org.hibernate.orm.test.mapping.converted.converter.registrations.MismatchDuplicateRegistrationTests}</li>
 * </ul>
 *
 * @author Steve Ebersole
 */
package org.hibernate.orm.test.mapping.converted.converter.registrations;

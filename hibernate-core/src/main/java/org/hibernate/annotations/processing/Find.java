/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations.processing;

import org.hibernate.Incubating;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.CLASS;

/**
 * Identifies a method of an abstract class or interface as defining
 * the signature of a finder method, and is generated automatically by
 * the Hibernate Metamodel Generator.
 *
 * @author Gavin King
 * @since 6.3
 */
@Target(METHOD)
@Retention(CLASS)
@Incubating
public @interface Find {}

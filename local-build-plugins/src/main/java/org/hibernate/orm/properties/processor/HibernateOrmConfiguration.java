/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.properties.processor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker annotation to define overrides configured by annotation processor configuration.
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ ElementType.TYPE, ElementType.FIELD })
public @interface HibernateOrmConfiguration {

	/**
	 * Describes to which type the configuration property belongs to - API/SPI.
	 */
	enum Type {
		/**
		 * Configuration property type API/SPI will be determined by inspecting the package in which a class is located.
		 * In case package contains {@code spi} package at any upper levels the type will be {@code SPI}, otherwise - {@code API}
		 */
		API,
		SPI
	}

	/**
	 * Set to {@code true} in case we have a {@code *Settings} class that we want to ignore in config processing.
	 * Also works on a field leve. Setting it to {@code true} on field level will not include that particular constant.
	 * Can be useful to skip prefix definitions etc.
	 */
	boolean ignore() default false;

	/**
	 * Overrides a prefix provided by annotation processor configuration. If set on class level - all constants from that class will
	 * use this prefix. If set on field level - that particular constant will use the configured prefix and will ignore the
	 * one set by annotation processor configuration or at class level.
	 */
	String[] prefix() default "";

	/**
	 * Used to group properties in sections and as a title of that grouped section.
	 */
	String title() default "";

	/**
	 * Used as part of generated anchor links to provide uniqueness.
	 */
	String anchorPrefix() default "";
}

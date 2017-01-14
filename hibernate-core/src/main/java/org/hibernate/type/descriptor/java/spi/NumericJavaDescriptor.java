/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor.java.spi;

/**
 * @author Steve Ebersole
 */
public interface NumericJavaDescriptor<T> extends BasicJavaDescriptor<T> {
	// todo : <T extends Number>?
	// 		^^ That limits us to JDK numeric types.  But without that there is not much
	//		else we can do with it...

	// todo : define standard toInt, toLong, toXyz... style methods
}

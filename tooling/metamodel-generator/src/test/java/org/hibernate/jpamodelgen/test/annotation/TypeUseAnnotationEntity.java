/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.test.annotation;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.sql.Blob;

/**
 * @author Valentin Rentschler
 */
@Entity
@Table
public class TypeUseAnnotationEntity {

	@Id
	private String id;

	@TypeUseAnnotation
	private String string;

	@TypeUseAnnotation
	private Blob blob;

	@TypeUseAnnotation
	private byte[] bytes;

	private byte[] bytesWoAnnotation;

	@TypeUseAnnotation
	private Byte[] bytesAlt;

	private Byte[] bytesAltWoAnnotation;

}



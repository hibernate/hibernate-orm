/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.basic;

import java.io.Serializable;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;

import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;

import org.hibernate.test.bytecode.enhancement.AbstractEnhancerTestTask;

/**
 * @author Luis Barreiro
 */
public class HHH9529TestTask extends AbstractEnhancerTestTask {

	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {Parent.class, Child.class, ChildKey.class};
	}

	public void prepare() {
		Configuration cfg = new Configuration();
		cfg.setProperty( Environment.ENABLE_LAZY_LOAD_NO_TRANS, "true" );
		cfg.setProperty( Environment.USE_SECOND_LEVEL_CACHE, "false" );
		super.prepare( cfg );
	}

	public void execute() {
	}

	protected void cleanup() {
	}

	@Entity
	public class Parent {
		@Id
		String id;
	}

	@Embeddable
	public class ChildKey implements Serializable {
		String parent;
		String type;
	}

	@Entity
	public class Child {
		@EmbeddedId
		ChildKey id;

		@MapsId("parent")
		@ManyToOne
		Parent parent;

		public String getfieldOnChildKeyParent() {
			// Note that there are two GETFIELD ops here, one on the field 'id' that should be enhanced and another
			// on the field 'parent' that may be or not (depending if 'extended enhancement' is enabled)

			// Either way, the field 'parent' on ChildKey should not be confused with the field 'parent' on Child

			return id.parent;
		}
	}
}

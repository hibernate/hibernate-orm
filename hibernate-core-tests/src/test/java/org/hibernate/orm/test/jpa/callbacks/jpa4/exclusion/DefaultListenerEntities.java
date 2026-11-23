package org.hibernate.orm.test.jpa.callbacks.jpa4.exclusion;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListener;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.ExcludeDefaultListeners;
import jakarta.persistence.ExcludeSuperclassListeners;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;

/// Fixtures loaded both from the classpath and from a real named module.
///
/// @author Steve Ebersole
public class DefaultListenerEntities {

	public static final List<String> EVENTS = new ArrayList<>();

	@MappedSuperclass
	@EntityListeners(SuperclassListener.class)
	public static class Base {
		@Id
		public int id;

		@PrePersist
		void callback() {
			EVENTS.add( "entity-callback" );
		}
	}

	@MappedSuperclass
	@ExcludeDefaultListeners
	public static class ExcludingBase extends Base {
	}

	@Entity(name = "DefaultListenerPlain")
	@EntityListeners(ExplicitListener.class)
	public static class Plain extends Base {
	}

	@Entity(name = "DefaultListenerExcluded")
	@EntityListeners(ExplicitListener.class)
	@ExcludeDefaultListeners
	public static class Excluded extends Base {
	}

	@Entity(name = "DefaultListenerInherited")
	@EntityListeners(ExplicitListener.class)
	public static class Inherited extends ExcludingBase {
	}

	@Entity(name = "DefaultListenerBothExcluded")
	@EntityListeners(ExplicitListener.class)
	@ExcludeDefaultListeners
	@ExcludeSuperclassListeners
	public static class BothExcluded extends Base {
	}

	@Entity(name = "DefaultListenerSuperclassExcluded")
	@EntityListeners(ExplicitListener.class)
	@ExcludeSuperclassListeners
	public static class SuperclassExcluded extends Base {
	}

	@Entity(name = "DefaultListenerInheritedWithSuperclassExcluded")
	@EntityListeners(ExplicitListener.class)
	@ExcludeSuperclassListeners
	public static class InheritedWithSuperclassExcluded extends ExcludingBase {
	}

	@Entity(name = "DefaultListenerXmlExcluded")
	@EntityListeners(ExplicitListener.class)
	public static class XmlExcluded extends Base {
	}

	public static class XmlListener {
		@PrePersist
		public void callback(Object entity) {
			EVENTS.add( "xml" );
		}
	}

	@EntityListener
	public static class DiscoveredListener {
		@PrePersist
		public void callback(Base entity) {
			EVENTS.add( "discovered" );
		}
	}

	public static class ModuleListener {
		@PrePersist
		public void callback(Object entity) {
			EVENTS.add( "module" );
		}
	}

	public static class PackageListener {
		@PrePersist
		public void callback(Object entity) {
			EVENTS.add( "package" );
		}
	}

	public static class SuperclassListener {
		@PrePersist
		public void callback(Object entity) {
			EVENTS.add( "superclass-listener" );
		}
	}

	public static class ExplicitListener {
		@PrePersist
		public void callback(Object entity) {
			EVENTS.add( "explicit-listener" );
		}
	}
}

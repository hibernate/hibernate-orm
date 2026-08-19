@jakarta.persistence.NamedQuery(name = "moduleQuery", query = "select e from ModuleAnnotationEntity e")
@jakarta.persistence.NamedNativeQuery(name = "moduleNativeQuery", query = "SELECT 1")
@jakarta.persistence.NamedStoredProcedureQuery(name = "moduleProc", procedureName = "my_procedure")
@org.hibernate.annotations.NamedQuery(name = "hqlModuleQuery", query = "select e from ModuleAnnotationEntity e")
@org.hibernate.annotations.NamedNativeQuery(name = "hqlModuleNativeQuery", query = "SELECT 1")
@org.hibernate.annotations.NamedEntityGraph(name = "moduleGraph", graph = "ModuleAnnotationEntity: id")
@org.hibernate.annotations.FilterDef(name = "moduleFilter")
@org.hibernate.annotations.FetchProfile(name = "moduleFetchProfile")
@org.hibernate.annotations.JavaTypeRegistration(
		javaType = org.hibernate.orm.integrationtest.java.module.test.annotation.StubDomainType.class,
		descriptorClass = org.hibernate.orm.integrationtest.java.module.test.annotation.StubJavaType.class
)
@org.hibernate.annotations.JdbcTypeRegistration(
		value = org.hibernate.type.descriptor.jdbc.VarcharJdbcType.class,
		registrationCode = 9999
)
@org.hibernate.annotations.ConverterRegistration(
		converter = org.hibernate.orm.integrationtest.java.module.test.annotation.StubConverter.class
)
@org.hibernate.annotations.TypeRegistration(
		basicClass = org.hibernate.orm.integrationtest.java.module.test.annotation.StubBasicType.class,
		userType = org.hibernate.orm.integrationtest.java.module.test.annotation.StubUserType.class
)
@org.hibernate.annotations.CollectionTypeRegistration(
		classification = org.hibernate.metamodel.CollectionClassification.BAG,
		type = org.hibernate.orm.integrationtest.java.module.test.annotation.StubUserCollectionType.class
)
@org.hibernate.annotations.CompositeTypeRegistration(
		embeddableClass = org.hibernate.orm.integrationtest.java.module.test.annotation.StubCompositeType.class,
		userType = org.hibernate.orm.integrationtest.java.module.test.annotation.StubCompositeUserType.class
)
@org.hibernate.annotations.EmbeddableInstantiatorRegistration(
		embeddableClass = org.hibernate.orm.integrationtest.java.module.test.annotation.StubEmbeddable.class,
		instantiator = org.hibernate.orm.integrationtest.java.module.test.annotation.StubEmbeddableInstantiator.class
)
@jakarta.persistence.EntityListeners(org.hibernate.orm.integrationtest.java.module.test.listener.ModuleListener.class)
module org.hibernate.orm.integrationtest.java.module.test {

	/*
	 * Main configuration, necessary for real client applications.
	 */

	opens org.hibernate.orm.integrationtest.java.module.test.entity to
			org.hibernate.orm.core,
			org.hibernate.accessor,
			org.hibernate.accessor.bytebuddy,
			org.hibernate.orm.envers;

	requires jakarta.persistence;
	// IDEA will not find the modules below because it apparently doesn't support automatic module names
	// for modules in the current project.
	// Everything should work fine when building from the command line, though.
	requires org.hibernate.orm.core;
	requires org.hibernate.accessor;
	requires org.hibernate.accessor.bytebuddy;
	requires org.hibernate.orm.envers;

	// Transitive dependencies that leak through the Hibernate ORM API
	requires java.sql;
	requires java.naming; // SessionFactory extends "javax.naming.Referenceable"

	/*
	 * Test-only configuration.
	 */

	opens org.hibernate.orm.integrationtest.java.module.test to org.junit.platform.commons;
	opens org.hibernate.orm.integrationtest.java.module.test.annotation to
			org.hibernate.orm.core;
	opens org.hibernate.orm.integrationtest.java.module.test.listener to
			org.hibernate.orm.core;
	requires org.junit.jupiter.api;
	requires org.hibernate.orm.scan.jandex;
	requires org.assertj.core;
}

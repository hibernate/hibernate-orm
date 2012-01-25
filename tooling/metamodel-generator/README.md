# Hibernate JPA 2 Metamodel Generator

## Latest stable version

*1.2.0.Final, 06.03.2012*

## What is it?

The Hibernate JPA 2 Metamodel Generator is a Java 6 annotation processor generating meta model classes for JPA 2 type-safe criteria queries.

The processor (*JPAMetaModelEntityProcessor*) processes classes annotated with *@Entity*, *@MappedSuperclass* or *@Embeddable*, as well as entities mapped in */META-INF/orm.xml* and mapping files specified in *persistence.xml*.

## System Requirements

JDK 1.6 or above.

## Licensing

Please see the file called license.txt

## Documentation

[JPA 2 Metamodel Generator Documentation](http://www.hibernate.org/subprojects/jpamodelgen/docs)

## Resources

* [Home Page](http://www.hibernate.org/subprojects/jpamodelgen.html)
* [Source Code](http://github.com/hibernate/hibernate-metamodelgen)
* [Mailing Lists](http://www.hibernate.org/community/mailinglists)
* [Issue Tracking](http://opensource.atlassian.com/projects/hibernate/browse/METAGEN)

## Build from source 

    git clone git@github.com:hibernate/hibernate-metamodelgen.git
    cd hibernate-metamodelgen
    mvn clean package


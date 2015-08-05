Status of the documentation overhaul (5.0 version)
==================================================

Overall the plan is to define 3 DocBook-based guides.  The intention is for this document to serve
as an outline of the work and a status of what still needs done.


User Guide
==========

Covers reference topics targeting users.

* Prefix (done)
* Architecture (done)
* DomainModel (done)
* Bootstrap (done)
* PersistenceContext (done)
* Database_Access (done)
* Transactions (done)
* JNDI (done)
* Locking (needs some work)
* Fetching (needs some work)
* Batching (needs lot of work - not started - open questions)
* Caching (needs some work)
* Events (need some work)
* HQL_JPQL (needs lots of work)
* Criteria (needs lots of work)
* Native_Queries (needs lots of work)
* Multi_Tenancy (needs some work)
* OSGi (right place for this?)
* Envers
* Portability (needs some work)


Domain Model Mapping Guide
===========================

Covers mapping domain model to database.  Note that a lot of the "not started" content exists elsewhere; its merely a 
matter of pulling that content in and better organizing it.
   

* Prefix (done)
* Data_Categorizations (done)
* Basic_Types (done)
* Composition (done)
* Collection (needs some work)
* Entity (needs some work)
* Secondary_Tables (not started)
* Identifiers (mostly done - needs "derived id" stuff documented)
* Natural_Id (not started)
* Associations (not started)
* Attribute_Access (not started)
* Mapping_Overrides - AttributeOverrides/AssociationOverrides (not started)
* Generated_attributes (not started)
* "columns, formulas, read/write-fragments" (not started)
* Naming_Strategies - implicit, physical, quoting (not started)
* Database_Constraints - pk, fk, uk, check, etc (not started)
* Auxiliary_DB_Objects - does this belong here?  or somewhere else (integrations guide) discussing schema tooling?


Integrations Guide
===================

* Services&Registries (pretty much done)
* IdGeneratorStrategyInterpreter (not started)
* custom Session/SessionFactory implementors (not started)
* ???
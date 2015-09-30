Status of the documentation overhaul (5.0 version)
==================================================

Overall the plan is to define 3 DocBook-based guides.  The intention is for this document to serve
as an outline of the work and a status of what still needs done.

NOTE : entries marked with <strike>strike-through</strike> indicate that the content is believed to be done; review 
would still be appreciated.


User Guide
==========

Covers reference topics targeting users.

* <strike>Prefix</strike>
* <strike>Architecture</strike>
* <strike>DomainModel</strike>
* <strike>Bootstrap</strike>
* <strike>PersistenceContext</strike>
* <strike>Database_Access</strike>
* <strike>Transactions</strike>
* <strike>JNDI</strike>
* Fetching - still need to document batch fetching, subselect fetching, extra laziness and EntityGraphs
* Flushing (to be written)
* Cascading (needs lots of work)
* Locking (needs some work)
* Batching (needs lot of work - not started - open questions)
* Caching (needs some work)
* Events (need some work)
* <strike>Query - HQL/JPQL</strike>
* <strike>Query - Criteria</strike>
* <strike>Query - Native (copy from old)</strike>
* Multi_Tenancy (needs some work)
* OSGi (right place for this?)
* Envers (right place for this?)
* Portability (needs some work)


Domain Model Mapping Guide
===========================

Covers mapping domain model to database.  Note that a lot of the "not started" content exists elsewhere; its merely a 
matter of pulling that content in and better organizing it.
   

* <strike>Prefix</strike>
* <strike>Data_Categorizations</strike>
* Entity (needs some work)
* <strike>Basic_Types</strike>
* <strike>Composition</strike>
* <strike>Collection (needs some work)
* Identifiers (mostly done - needs "derived id" stuff documented)
* <strike>Natural_Id</strike>
* Secondary_Tables (not started) - logically a joined in-line view
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


Overall
=======

* I really like the idea of each chapter having a title+abstract.  See userGuide/chapters/HQL.xml 
	for an example.
* I really like the idea of each chapter having a "Related Topics" (?)sidebar(?).  See 
	userGuide/chapters/HQL.xml for an example.  I am not sure `<sidebar/>` is the best element for
	this concept, but I could not find a better one on cursory glance.  I noticed `literallayout` used in
	a few DocBook examples for something similar.

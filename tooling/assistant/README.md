<!--
  ~ Copyright 2010 - 2025 Red Hat, Inc.
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~     http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" basis,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  -->

[![Hibernate](https://static.jboss.org/hibernate/images/hibernate_200x150.png)](https://tools.hibernate.org)

# Hibernate Tools for Natural Language

This project contains the `HibernateAssistant` interface, which is aimed at providing a natural language interface to Hibernate ORM's persistence capabilities, through the capabilities of modern LLMs. **Its implementation is not included here**, but different providers can implement this interface with their own logic to interact with the underlying Generative AI model, taking advantage of the building blocks and utilities that _are_ included here.

To be able to interact with different model providers, the `MetamodelSerializer` and `ResultsSerializer` SPIs can be used to generate a textual (JSON) representation of Hibernate's mapping model and data. This text can be easily fed to an LLM that will enable interacting with your database through simple natural language interactions.

WARNING: This entire module is currently incubating and may experience breaking changes at any time, including in a micro (patch) release.
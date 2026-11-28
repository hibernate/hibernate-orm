package org.hibernate.orm.test.boot.models.xml;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.OneToOne;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.Table;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Verifies that orm.xml `<primary-key-join-column/>` and `<primary-key-foreign-key/>`
/// reach the boot model, the same way `@PrimaryKeyJoinColumn` and `foreignKey` members do,
/// for the key of a `JOINED` subclass and for the key of a secondary table.
@JiraKey("HHH-21252")
public class XmlPrimaryKeyJoinColumnTest {

	@Test
	void joinedSubclassKeyColumnNameIsApplied() {
		final PersistentClass dog = bindJoinedSubclass( "<primary-key-join-column name=\"dog_pk\"/>" );

		assertThat( columnNames( dog.getKey().getSelectables() ) ).containsExactly( "dog_pk" );
	}

	@Test
	void joinedSubclassNestedForeignKeyNameIsApplied() {
		final PersistentClass dog = bindJoinedSubclass( """
				<primary-key-join-column name="dog_pk">
					<foreign-key name="fk_dog_nested"/>
				</primary-key-join-column>
				""" );

		assertThat( foreignKeyNames( dog.getTable() ) ).containsExactly( "fk_dog_nested" );
	}

	@Test
	void joinedSubclassPrimaryKeyForeignKeyNameIsApplied() {
		final PersistentClass dog = bindJoinedSubclass( """
				<primary-key-join-column name="dog_pk"/>
				<primary-key-foreign-key name="fk_dog_entity"/>
				""" );

		assertThat( foreignKeyNames( dog.getTable() ) ).containsExactly( "fk_dog_entity" );
	}

	@Test
	void joinedSubclassPrimaryKeyForeignKeyWithoutJoinColumnDefaultsTheKeyColumn() {
		final PersistentClass dog = bindJoinedSubclass( "<primary-key-foreign-key name=\"fk_dog_entity\"/>" );

		assertThat( columnNames( dog.getKey().getSelectables() ) ).containsExactly( "id" );
		assertThat( foreignKeyNames( dog.getTable() ) ).containsExactly( "fk_dog_entity" );
	}

	@Test
	void secondaryTableKeyColumnNameIsApplied() {
		final Join join = bindSecondaryTable( "<primary-key-join-column name=\"detail_owner_id\"/>" );

		assertThat( columnNames( join.getKey().getSelectables() ) ).containsExactly( "detail_owner_id" );
	}

	@Test
	void secondaryTableNestedForeignKeyNameIsApplied() {
		final Join join = bindSecondaryTable( """
				<primary-key-join-column name="detail_owner_id">
					<foreign-key name="fk_detail_nested"/>
				</primary-key-join-column>
				""" );

		assertThat( foreignKeyNames( join.getTable() ) ).containsExactly( "fk_detail_nested" );
	}

	@Test
	void secondaryTablePrimaryKeyForeignKeyNameIsApplied() {
		final Join join = bindSecondaryTable( """
				<primary-key-join-column name="detail_owner_id"/>
				<primary-key-foreign-key name="fk_detail_table"/>
				""" );

		assertThat( foreignKeyNames( join.getTable() ) ).containsExactly( "fk_detail_table" );
	}

	@Test
	void secondaryTableForeignKeyTakesPrecedenceOverPrimaryKeyForeignKey() {
		final Join join = bindSecondaryTable( """
				<primary-key-join-column name="detail_owner_id"/>
				<primary-key-foreign-key name="fk_detail_pk"/>
				<foreign-key name="fk_detail_table"/>
				""" );

		assertThat( foreignKeyNames( join.getTable() ) ).containsExactly( "fk_detail_table" );
	}

	@Test
	void oneToOnePrimaryKeyForeignKeyWithoutJoinColumnMapsToThePrimaryKey() {
		final PersistentClass passport = bindOneToOne( "<primary-key-foreign-key name=\"fk_passport_person\"/>" );

		assertThat( passport.getProperty( "person" ).getValue() ).isInstanceOf( OneToOne.class );
		assertThat( passport.getTable().getColumns() ).extracting( Column::getName ).containsExactly( "id" );
	}

	@Test
	void oneToOneWithoutPrimaryKeyJoinElementsMapsToAForeignKeyColumn() {
		final PersistentClass passport = bindOneToOne( "" );

		assertThat( passport.getProperty( "person" ).getValue() ).isInstanceOf( ManyToOne.class );
		assertThat( passport.getTable().getColumns() ).extracting( Column::getName )
				.containsExactlyInAnyOrder( "id", "person_id" );
	}

	private static PersistentClass bindOneToOne(String primaryKeyJoinElements) {
		final String xml = """
				<entity-mappings xmlns="https://www.hibernate.org/xsd/orm/mapping" version="7.0">
					<entity class="%s" name="Person" access="FIELD" metadata-complete="true">
						<table name="person"/>
						<attributes>
							<id name="id"/>
						</attributes>
					</entity>
					<entity class="%s" name="Passport" access="FIELD" metadata-complete="true">
						<table name="passport"/>
						<attributes>
							<id name="id"/>
							<one-to-one name="person">
								%s
							</one-to-one>
						</attributes>
					</entity>
				</entity-mappings>
				""".formatted( Person.class.getName(), Passport.class.getName(), primaryKeyJoinElements );
		return buildMetadata( xml ).getEntityBinding( Passport.class.getName() );
	}

	private static PersistentClass bindJoinedSubclass(String primaryKeyJoinElements) {
		final String xml = """
				<entity-mappings xmlns="https://www.hibernate.org/xsd/orm/mapping" version="7.0">
					<entity class="%s" name="Animal" access="FIELD" metadata-complete="true">
						<table name="animal"/>
						<inheritance strategy="JOINED"/>
						<attributes>
							<id name="id"/>
						</attributes>
					</entity>
					<entity class="%s" name="Dog" access="FIELD" metadata-complete="true">
						<table name="dog"/>
						%s
						<attributes>
							<basic name="breed"/>
						</attributes>
					</entity>
				</entity-mappings>
				""".formatted( Animal.class.getName(), Dog.class.getName(), primaryKeyJoinElements );
		return buildMetadata( xml ).getEntityBinding( Dog.class.getName() );
	}

	private static Join bindSecondaryTable(String primaryKeyJoinElements) {
		final String xml = """
				<entity-mappings xmlns="https://www.hibernate.org/xsd/orm/mapping" version="7.0">
					<entity class="%s" name="Owner" access="FIELD" metadata-complete="true">
						<table name="owner"/>
						<secondary-table name="owner_detail">
							%s
						</secondary-table>
						<attributes>
							<id name="id"/>
							<basic name="detail">
								<column table="owner_detail"/>
							</basic>
						</attributes>
					</entity>
				</entity-mappings>
				""".formatted( Owner.class.getName(), primaryKeyJoinElements );
		final PersistentClass owner = buildMetadata( xml ).getEntityBinding( Owner.class.getName() );
		assertThat( owner.getJoins() ).as( "Secondary tables declared in orm.xml" ).hasSize( 1 );
		return owner.getJoins().get( 0 );
	}

	private static Metadata buildMetadata(String xml) {
		try ( var registry = new StandardServiceRegistryBuilder()
				.applySetting( "hibernate.dialect", H2Dialect.class.getName() )
				.applySetting( "hibernate.boot.allow_jdbc_metadata_access", false )
				.applySetting( "hibernate.validate_xml", true )
				.build() ) {
			return new MetadataSources( registry )
					.addInputStream( new ByteArrayInputStream( xml.getBytes( StandardCharsets.UTF_8 ) ) )
					.buildMetadata();
		}
	}

	private static String[] columnNames(Iterable<Selectable> selectables) {
		final var names = new ArrayList<String>();
		for ( final Selectable selectable : selectables ) {
			names.add( selectable.getText() );
		}
		return names.toArray( String[]::new );
	}

	private static String[] foreignKeyNames(Table table) {
		final var names = new ArrayList<String>();
		for ( final ForeignKey foreignKey : table.getForeignKeyCollection() ) {
			names.add( foreignKey.getName() );
		}
		return names.toArray( String[]::new );
	}

	public static class Animal {
		Long id;
	}

	public static class Dog extends Animal {
		String breed;
	}

	public static class Person {
		Long id;
	}

	public static class Passport {
		Long id;
		Person person;
	}

	public static class Owner {
		Long id;
		String detail;
	}
}

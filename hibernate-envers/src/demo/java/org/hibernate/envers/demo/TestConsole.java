/*
 * Envers. http://www.jboss.org/envers
 *
 * Copyright 2008  Red Hat Middleware, LLC. All rights reserved.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT A WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License, v.2.1 along with this distribution; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 *
 * Red Hat Author(s): Adam Warski
 */
package org.hibernate.envers.demo;

import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.DefaultRevisionEntity;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.util.*;
import java.io.PrintStream;
import java.io.File;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class TestConsole {
    private EntityManager entityManager;

    public TestConsole(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    private String convertString(String s, String def) {
        if ("NULL".equals(s)) { return null; }
        if ("".equals(s)) { return def; }
        return s;
    }

    private int convertStringToInteger(String s, int def) {
        if ("".equals(s)) { return def; }
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            System.err.println("Invalid number, returning 0.");
            return 0;
        }
    }

    private void printPerson(StringBuilder sb, Person p) {
        sb.append("id = ").append(p.getId()).append(", name = ").append(p.getName())
                .append(", surname = ").append(p.getSurname());

        Address a = p.getAddress();
        if (a != null) {
            sb.append(", address = <").append(a.getId()).append("> ").append(a.getStreetName()).append(" ")
                    .append(a.getHouseNumber()).append("/").append(a.getFlatNumber());
        }
    }

    @SuppressWarnings({"unchecked"})
    private void printPersons(StringBuilder sb) {
        List<Person> persons = entityManager.createQuery(
                "select p from Person p order by p.id").getResultList();

        sb.append("Persons:\n");
        for (Person p : persons) {
            printPerson(sb, p);
            sb.append("\n");
        }
    }

    private void printPersonHistory(StringBuilder sb, int personId) {
        AuditReader reader = AuditReaderFactory.get(entityManager);

        List personHistory = reader.createQuery()
                .forRevisionsOfEntity(Person.class, false, true)
                .add(AuditEntity.id().eq(personId))
                .getResultList();

        if (personHistory.size() == 0) {
            sb.append("A person with id ").append(personId).append(" does not exist.\n");
        } else {
            for (Object historyObj : personHistory) {
                Object[] history = (Object[]) historyObj;
                DefaultRevisionEntity revision = (DefaultRevisionEntity) history[1];
                sb.append("revision = ").append(revision.getId()).append(", ");
                printPerson(sb, (Person) history[0]);
                sb.append(" (").append(revision.getRevisionDate()).append(")\n");
            }
        }
    }

    private void printPersonAtRevision(StringBuilder sb, int personId, int revision) {
        AuditReader reader = AuditReaderFactory.get(entityManager);

        Person p = reader.find(Person.class, personId, revision);
        if (p == null) {
            sb.append("This person does not exist at that revision.");
        } else {
            printPerson(sb, p);
        }
    }

    private void readAndSetAddress(Scanner scanner, Person p) {
        Address old = p.getAddress();

        String input = scanner.nextLine();
        if ("NULL".equals(input)) {
            p.setAddress(null);
            if (old != null) {
                old.getPersons().remove(p);
            }
        } else if ("".equals(input)) {
        } else {
            try {
                Integer id = Integer.valueOf(input);

                Address a = entityManager.find(Address.class, id);

                if (a == null) {
                    System.err.println("Unknown address id, setting to NULL.");
                    p.setAddress(null);
                    if (old != null) {
                        old.getPersons().remove(p);
                    }
                } else {
                    p.setAddress(a);

                    a.getPersons().add(p);

                    if (old != null) {
                        old.getPersons().remove(p);
                    }
                }
            } catch (NumberFormatException e) {
                System.err.println("Invalid address id, setting to NULL.");
                p.setAddress(null);
                if (old != null) {
                    old.getPersons().remove(p);
                }
            }
        }
    }

    private Person readNewPerson(PrintStream out, Scanner scanner) {
        Person p = new Person();

        out.print("Person name (NULL for null): ");
        p.setName(convertString(scanner.nextLine(), ""));

        out.print("Person surname (NULL for null): ");
        p.setSurname(convertString(scanner.nextLine(), ""));

        out.print("Person address id (NULL for null): ");
        readAndSetAddress(scanner, p);

        return p;
    }

    private void readModifyPerson(PrintStream out, Scanner scanner, int personId) {
        Person current = entityManager.find(Person.class, personId);

        if (current == null) {
            out.println("Person with id " + personId + " does not exist.");
            return;
        }

        out.print("Person name (NULL for null, enter for no change, current - " + current.getName() + "): ");
        current.setName(convertString(scanner.nextLine(), current.getName()));

        out.print("Person surname (NULL for null, enter for no change, current - " + current.getSurname() + "): ");
        current.setSurname(convertString(scanner.nextLine(), current.getSurname()));

        out.print("Person address id (NULL for null, enter for no change, current - " +
                (current.getAddress() == null ? "NULL" : current.getAddress().getId()) + "): ");
        readAndSetAddress(scanner, current);
    }

    private void printAddress(StringBuilder sb, Address a) {
        sb.append("id = ").append(a.getId()).append(", streetName = ").append(a.getStreetName())
                .append(", houseNumber = ").append(a.getHouseNumber())
                .append(", flatNumber = ").append(a.getFlatNumber())
                .append(", persons = (");

        Iterator<Person> iter = a.getPersons().iterator();
        while (iter.hasNext()) {
            Person p = iter.next();
            sb.append("<").append(p.getId()).append("> ").append(p.getName()).append(" ").append(p.getSurname());
            if (iter.hasNext()) {
                sb.append(", ");
            }
        }

        sb.append(")");
    }

    @SuppressWarnings({"unchecked"})
    private void printAddresses(StringBuilder sb) {
        List<Address> addresses = entityManager.createQuery(
                "select a from Address a order by a.id").getResultList();

        sb.append("Addresses:\n");
        for (Address a : addresses) {
            printAddress(sb, a);
            sb.append("\n");
        }
    }

    private void printAddressHistory(StringBuilder sb, int addressId) {
        AuditReader reader = AuditReaderFactory.get(entityManager);

        List addressHistory = reader.createQuery()
                .forRevisionsOfEntity(Address.class, false, true)
                .add(AuditEntity.id().eq(addressId))
                .getResultList();

        if (addressHistory.size() == 0) {
            sb.append("A address with id ").append(addressId).append(" does not exist.\n");
        } else {
            for (Object historyObj : addressHistory) {
                Object[] history = (Object[]) historyObj;
                DefaultRevisionEntity revision = (DefaultRevisionEntity) history[1];
                sb.append("revision = ").append(revision.getId()).append(", ");
                printAddress(sb, (Address) history[0]);
                sb.append(" (").append(revision.getRevisionDate()).append(")\n");
            }
        }
    }

    private void printAddressAtRevision(StringBuilder sb, int addressId, int revision) {
        AuditReader reader = AuditReaderFactory.get(entityManager);

        Address a = reader.find(Address.class, addressId, revision);
        if (a == null) {
            sb.append("This address does not exist at that revision.");
        } else {
            printAddress(sb, a);
        }
    }

    private Address readNewAddress(PrintStream out, Scanner scanner) {
        Address a = new Address();

        out.print("Street name (NULL for null): ");
        a.setStreetName(convertString(scanner.nextLine(), ""));

        out.print("House number: ");
        a.setHouseNumber(convertStringToInteger(scanner.nextLine(), 0));

        out.print("Flat number: ");
        a.setFlatNumber(convertStringToInteger(scanner.nextLine(), 0));

        a.setPersons(new HashSet<Person>());

        return a;
    }

    private void readModifyAddress(PrintStream out, Scanner scanner, int addressId) {
        Address current = entityManager.find(Address.class, addressId);

        if (current == null) {
            out.println("Address with id " + addressId + " does not exist.");
            return;
        }

        out.print("Street name (NULL for null, enter for no change, current - " + current.getStreetName() + "): ");
        current.setStreetName(convertString(scanner.nextLine(), current.getStreetName()));

        out.print("House number (enter for no change, current - " + current.getHouseNumber() + "): ");
        current.setHouseNumber(convertStringToInteger(scanner.nextLine(), current.getHouseNumber()));

        out.print("Flat number (enter for no change, current - " + current.getFlatNumber() + "): ");
        current.setFlatNumber(convertStringToInteger(scanner.nextLine(), current.getFlatNumber()));
    }

    private void start() {
        Scanner scanner = new Scanner(System.in);
        PrintStream out = System.out;

        while (true) {
            out.println("-----------------------------------------------");
            out.println("1 - list persons             5 - list addresses");
            out.println("2 - list person history      6 - list addresses history");
            out.println("3 - new person               7 - new address");
            out.println("4 - modify person            8 - modify address");
            out.println("9 - get person at revision  10 - get address at revision");
            out.println("                             0 - end");

            try {
                int choice = scanner.nextInt();

                scanner.nextLine();

                entityManager.getTransaction().begin();

                StringBuilder sb;
                int personId;
                int addressId;
                int revision;
                switch (choice) {
                    case 1:
                        sb = new StringBuilder();
                        printPersons(sb);
                        out.println(sb.toString());
                        break;
                    case 2:
                        out.print("Person id: ");
                        personId = scanner.nextInt(); scanner.nextLine();
                        sb = new StringBuilder();
                        printPersonHistory(sb, personId);
                        out.println(sb.toString());
                        break;
                    case 3:
                        Person p = readNewPerson(out, scanner);
                        entityManager.persist(p);
                        break;
                    case 4:
                        out.print("Person id: ");
                        personId = scanner.nextInt(); scanner.nextLine();
                        readModifyPerson(out, scanner, personId);
                        break;
                    case 5:
                        sb = new StringBuilder();
                        printAddresses(sb);
                        out.println(sb.toString());
                        break;
                    case 6:
                        out.print("Address id: ");
                        addressId = scanner.nextInt(); scanner.nextLine();
                        sb = new StringBuilder();
                        printAddressHistory(sb, addressId);
                        out.println(sb.toString());
                        break;
                    case 7:
                        Address a = readNewAddress(out, scanner);
                        entityManager.persist(a);
                        break;
                    case 8:
                        out.print("Address id: ");
                        addressId = scanner.nextInt(); scanner.nextLine();
                        readModifyAddress(out, scanner, addressId);
                        break;
                    case 9:
                        out.print("Person id: ");
                        personId = scanner.nextInt(); scanner.nextLine();
                        out.print("Revision number: ");
                        revision = scanner.nextInt(); scanner.nextLine();
                        if (revision <= 0) {
                            System.out.println("Revision must be greater then 0!");
                            continue;
                        }
                        sb = new StringBuilder();
                        printPersonAtRevision(sb, personId, revision);
                        out.println(sb.toString());
                        break;
                    case 10:
                        out.print("Address id: ");
                        addressId = scanner.nextInt(); scanner.nextLine();
                        out.print("Revision number: ");
                        revision = scanner.nextInt(); scanner.nextLine();
                        if (revision <= 0) {
                            System.out.println("Revision must be greater then 0!");
                            continue;
                        }
                        sb = new StringBuilder();
                        printAddressAtRevision(sb, addressId, revision);
                        out.println(sb.toString());
                        break;

                    case 0:
                        return;
                }
            } catch (InputMismatchException e) {
                // continuing
            } finally {
                entityManager.getTransaction().commit();
            }
        }
    }

    private boolean hasData() {
        return (((Long) entityManager.createQuery("select count(a) from Address a").getSingleResult()) +
                ((Long) entityManager.createQuery("select count(p) from Person p").getSingleResult())) > 0;
    }

    private void populateTestData() {
        entityManager.getTransaction().begin();

        if (!hasData()) {
            Person p1 = new Person();
            Person p2 = new Person();
            Person p3 = new Person();

            Address a1 = new Address();
            Address a2 = new Address();

            p1.setName("James");
            p1.setSurname("Bond");
            p1.setAddress(a1);

            p2.setName("John");
            p2.setSurname("McClane");
            p2.setAddress(a2);

            p3.setName("Holly");
            p3.setSurname("Gennaro");
            p3.setAddress(a2);

            a1.setStreetName("MI6");
            a1.setHouseNumber(18);
            a1.setFlatNumber(25);
            a1.setPersons(new HashSet<Person>());
            a1.getPersons().add(p1);

            a2.setStreetName("Nakatomi Plaza");
            a2.setHouseNumber(10);
            a2.setFlatNumber(34);
            a2.setPersons(new HashSet<Person>());
            a2.getPersons().add(p2);
            a2.getPersons().add(p3);

            entityManager.persist(a1);
            entityManager.persist(a2);

            entityManager.persist(p1);
            entityManager.persist(p2);
            entityManager.persist(p3);

            System.out.println("The DB was populated with example data.");
        }

        entityManager.getTransaction().commit();
    }

    public static void main(String[] args) {
        String userDbFile = System.getProperty("java.io.tmpdir") + File.separator + "_versions_demo.db";

        Map<String, String> configurationOverrides = new HashMap<String, String>();
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("ConsolePU", configurationOverrides);
        EntityManager entityManager = emf.createEntityManager();

        TestConsole console = new TestConsole(entityManager);

        System.out.println("");
        System.out.println("Welcome to EntityVersions demo!");
        System.out.println("HSQLDB database file location: " + userDbFile);

        console.populateTestData();
        console.start();

        entityManager.close();
        emf.close();
    }
}

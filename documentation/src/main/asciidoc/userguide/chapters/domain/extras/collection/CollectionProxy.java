@Entity
public class Person  {

    @Id
    private Long id;

    @ElementCollection
    private List<String> phones = new ArrayList<>();

    public List<String> getPhones() {
        return phones;
    }
}

Person person = entityManager.find(Person.class, 1L);
//Throws java.lang.ClassCastException: org.hibernate.collection.internal.PersistentBag cannot be cast to java.util.ArrayList
ArrayList<String> phones = (ArrayList<String>) person.getPhones();
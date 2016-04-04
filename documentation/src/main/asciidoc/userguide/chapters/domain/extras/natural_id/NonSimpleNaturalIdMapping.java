@Entity
public class Course {

    @Id
    private Integer id;

    @NaturalId
    @ManyToOne
    private Department department;

    @NaturalId
    private String code;

    ...
}
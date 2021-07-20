package life.genny.entity;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;

@Entity
public class DummyEntity {
    private Long id;
    private String name;

    @Id
    @SequenceGenerator(name = "deSeq", sequenceName = "de_id_seq", allocationSize = 1, initialValue = 1)
    @GeneratedValue(generator = "deSeq")
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

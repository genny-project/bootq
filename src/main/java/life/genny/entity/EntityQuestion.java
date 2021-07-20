package life.genny.entity;


import java.lang.invoke.MethodHandles;

import org.apache.logging.log4j.Logger;

import com.google.gson.annotations.Expose;

public class EntityQuestion implements java.io.Serializable, Comparable<Object> {

    private static final long serialVersionUID = 1L;

    protected static final Logger log = org.apache.logging.log4j.LogManager
            .getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

    @Expose
    private String valueString;

    @Expose
    private Double weight;

    @Expose
    private Link link;

    public EntityQuestion() {
    }

    public EntityQuestion(Link link) {
        this.link = link;
    }

    @Override
    public int compareTo(Object o) {
        return 0;
    }
}

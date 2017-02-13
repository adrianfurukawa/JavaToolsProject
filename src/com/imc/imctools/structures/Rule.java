package com.imc.imctools.structures;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by faizal on 12/29/13.
 */
public class Rule {

    private List<Section> sections = new ArrayList<Section>();
    private Map<String,Section> sectionsMap = new HashMap<String,Section>();

    public void addSection(Section section) {
        sectionsMap.put(section.getName(), section);
        sections.add(section);
    }

    public Section getSection(String name) {
        return sectionsMap.get(name);
    }

    public List<Section> getSections() {
        return sections;
    }

    public void setSections(List<Section> sections) {
        this.sections = sections;
    }

}

package com.planitsquare.assignment_jaehyuk.util;

import lombok.experimental.UtilityClass;

import java.util.Arrays;
import java.util.List;

@UtilityClass
public class StringArrayUtils {

    public List<String> splitToList(String str) {
        return str != null && !str.isEmpty() ?
                Arrays.asList(str.split(",")) : List.of();
    }

    public String joinFromList(List<String> list) {
        return list != null && !list.isEmpty() ?
                String.join(",", list) : null;
    }
}